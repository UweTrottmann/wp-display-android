// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2022 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.dtareader

import okio.BufferedSource
import okio.IOException
import okio.buffer
import okio.source
import java.io.InputStream
import java.net.URL
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and

/**
 * Parses a DTA file with version [DtaFileReader.VERSION_9003].
 *
 * ```
 * val reader = DtaFileReader()
 * val loggerFileStream = reader.getLoggerFileStream(host, 15_000, 20_000)
 * val dtaFile = reader.readLoggerFile(loggerFileStream)
 * ```
 *
 * Based upon https://sourceforge.net/p/opendta/git/ci/master/tree/dtafile/dtafile9003.cpp
 */
class DtaFileReader {

    fun getUrl(host: String) = "http://$host/NewProc"

    /**
     * Opens an HTTP (not encrypted) connection to the host to get the DTA statistics file.
     *
     * Throws [java.io.IOException] if opening the connection fails, also if connecting or a read
     * takes longer than the given timeouts.
     */
    fun getLoggerFileStream(host: String, connectTimeoutMs: Int, readTimeoutMs: Int): InputStream {
        return URL(getUrl(host)).openConnection()
            .apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
            }
            .getInputStream()
    }

    /**
     * Parses the header and data sets into a [DtaFile].
     *
     * @throws java.io.IOException If reading fails due to any number of reasons (file structure
     * not as expected, version not as expected, file size not as expected).
     */
    fun readLoggerFile(inputStream: InputStream): DtaFile {
        try {
            inputStream.source().use { source ->
                source.buffer().use { bufferedSource ->
                    // Byte [0:3]: version
                    val version = bufferedSource.readIntLe()
                    if (version != VERSION_9003) {
                        throw IOException("Version is not $VERSION_9003")
                    }

                    val header = parseHeader(bufferedSource)
                    val datasets = parseDataSets(bufferedSource, header)

                    return DtaFile(
                        version,
                        header.fields,
                        datasets
                    )
                }
            }
        } catch (e: BufferUnderflowException) {
            throw IOException("File structure not as expected", e)
        }
    }

    private data class Header(
        val fields: List<ReadableField>,
        val datasetsToRead: Int,
        val datasetLength: Int
    )

    /**
     * @throws java.nio.BufferUnderflowException Thrown by [ByteBuffer.get], [readString] or
     * [readColor].
     */
    private fun parseHeader(bufferedSource: BufferedSource): Header {
        // Byte [4:7]: size of header
        val headerSize = bufferedSource.readIntLe()
        if (headerSize < 4) {
            // At least number and length (each short, 2 bytes) of datasets should be there
            throw IOException("Header size $headerSize is too small")
        }
        if (!bufferedSource.request(headerSize.toLong())) {
            throw IOException("Header is not $headerSize bytes long")
        }
        val headerBytes = bufferedSource.readByteArray(headerSize.toLong())
        val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Byte [8:9]: number of data sets (unsigned)
        val datasetsToRead = headerBuffer.short.toUShort().toInt()
        // Byte [10:11]: length of a data set (unsigned)
        val datasetLength = headerBuffer.short.toUShort().toInt()
        if (datasetLength < 6) {
            throw IOException("Data set length is smaller than 6 bytes (at least timestamp + 2 byte field)")
        }

        val fields = mutableListOf<ReadableField>()
        var index = 0
        var category = ""
        while (headerBuffer.hasRemaining()) {
            val fieldId = headerBuffer.get()
            when (val fieldType = fieldId and 0x0F) {
                0x00.toByte() -> {
                    // Category
                    category = readString(headerBuffer)
                }

                0x01.toByte() -> {
                    // Analogue field
                    val name = readString(headerBuffer)
                    val color = readColor(headerBuffer)
                    val factor = if (fieldId and 0x80.toByte() != 0x0.toByte()) {
                        // unsigned
                        headerBuffer.short.toUShort().toInt()
                    } else 10
                    if (factor == 0) {
                        // Don't allow infinite values (values are divided by the factor)
                        throw IOException("Analogue field $name has factor 0")
                    }
                    fields.add(AnalogueField(index++, category, name, color, factor))
                }

                0x02.toByte(), 0x04.toByte() -> {
                    // Digital field
                    val count = headerBuffer.get().toUByte().toInt()
                    if (count > MAX_DIGITAL_VALUES) {
                        // Each value is a bit of a 2 byte data set value
                        throw IOException("Digital field has $count values, more than $MAX_DIGITAL_VALUES")
                    }
                    val visibility = if (fieldId and 0x40.toByte() != 0x0.toByte()) {
                        headerBuffer.short
                    } else 0xFFFF.toShort() // All visible.

                    val customerSupportOnly = if (fieldId and 0x20.toByte() != 0x0.toByte()) {
                        headerBuffer.short
                    } else 0x0.toShort() // None intended for customer support.

                    val directions = if (fieldId and 0x04.toByte() != 0x0.toByte()) {
                        headerBuffer.short
                    } else if (fieldId and 0x80.toByte() != 0x0.toByte()) {
                        // All values are outputs.
                        0xFFFF.toShort()
                    } else {
                        // All values are inputs.
                        0x0.toShort()
                    }

                    val values = mutableListOf<DigitalValue>()
                    for (i in 0 until count) {
                        val name = readString(headerBuffer)
                        val color = readColor(headerBuffer)
                        val type = if (directions.toInt() and (1 shl i) != 0) {
                            DigitalType.OUTPUT
                        } else {
                            DigitalType.INPUT
                        }
                        values.add(
                            DigitalValue(
                                category,
                                name,
                                color,
                                visibility.toInt() and (1 shl i) != 0,
                                customerSupportOnly.toInt() and (1 shl i) != 0,
                                type
                            )
                        )
                    }
                    fields.add(DigitalField(index++, values))
                }

                0x03.toByte() -> {
                    // Enum field
                    val name = readString(headerBuffer)
                    val count = headerBuffer.get().toUByte().toInt()

                    val enumValues = mutableListOf<String>()
                    for (i in 0 until count) {
                        val enumValue = readString(headerBuffer)
                        enumValues.add(enumValue)
                    }

                    fields.add(EnumField(index++, name, enumValues))
                }

                else -> throw IOException("Unknown field type $fieldType")
            }
        }

        // Check number of fields * 2 (length of value) == data set length
        val expectedDataSetLength = fields.size * 2 + 4 // 4 byte time stamp
        if (expectedDataSetLength != datasetLength) {
            throw IOException("Announced data set length ($datasetLength bytes) does not match fields ($expectedDataSetLength bytes)")
        }

        return Header(
            fields,
            datasetsToRead,
            datasetLength
        )
    }

    private fun parseDataSets(bufferedSource: BufferedSource, header: Header): List<DataSet> {
        val dataSetLength = header.datasetLength
        val datasets = mutableListOf<DataSet>()
        for (i in 0 until header.datasetsToRead) {
            if (!bufferedSource.request(dataSetLength.toLong())) {
                throw IOException("Data set $i is not $dataSetLength bytes long.")
            }
            val bytes = bufferedSource.readByteArray(dataSetLength.toLong())
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            // First 4 bytes are unix time in seconds (unsigned, so works after 2038)
            val epochSecond = buffer.int.toUInt().toLong()
            // Then for each field 2 bytes
            val fieldValues = mutableListOf<List<Double>>()
            header.fields.forEach { fieldValues.add(it.readValue(buffer)) }

            datasets.add(
                DataSet(
                    epochSecond,
                    fieldValues
                )
            )
        }
        return datasets
    }

    /**
     * @throws java.nio.BufferUnderflowException Thrown by [ByteBuffer.get].
     */
    private fun readString(buffer: ByteBuffer): String {
        var string = ""
        while (true) {
            val char = buffer.get()
            if (char == 0x0.toByte()) {
                break
            } else {
                // Unsigned, so bytes >= 0x80 map to Latin-1 characters (like 0xFC to ü)
                string += char.toUByte().toInt().toChar()
            }
        }
        return string
    }

    /**
     * Reads 3 bytes (red, green, blue) and returns them as an opaque ARGB color.
     *
     * @throws java.nio.BufferUnderflowException Thrown by [ByteBuffer.get].
     */
    internal fun readColor(buffer: ByteBuffer): Int {
        // Unsigned to avoid sign extension of bytes >= 0x80
        val r = buffer.get().toUByte().toInt()
        val g = buffer.get().toUByte().toInt()
        val b = buffer.get().toUByte().toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    companion object {
        const val VERSION_9003 = 9003

        /** A digital field stores one bit per value in a 2 byte data set value. */
        private const val MAX_DIGITAL_VALUES = 16
    }

    data class DtaFile(
        val version: Int,
        val fields: List<ReadableField>,
        val datasets: List<DataSet>
    ) {
        val analogueFields: List<AnalogueField> = fields.filterIsInstance<AnalogueField>()
        val digitalFields: List<DigitalField> = fields.filterIsInstance<DigitalField>()
        val enumFields: List<EnumField> = fields.filterIsInstance<EnumField>()
    }

    interface ReadableField {
        val index: Int
        fun readValue(byteBuffer: ByteBuffer): List<Double>
    }

    data class AnalogueField(
        override val index: Int,
        val category: String,
        val name: String,
        val color: Int,
        /** Unsigned 16-bit value, the raw value is divided by it. */
        val factor: Int
    ) : ReadableField {
        override fun readValue(byteBuffer: ByteBuffer): List<Double> {
            val value = byteBuffer.short
            return listOf(value.toDouble() / factor)
        }
    }

    data class DigitalField(
        override val index: Int,
        val values: List<DigitalValue>
    ) : ReadableField {
        override fun readValue(byteBuffer: ByteBuffer): List<Double> {
            val value = byteBuffer.short.toInt()
            return List(values.size) { index ->
                if (value and (1 shl index) != 0) 1.0 else 0.0
            }
        }
    }

    data class DigitalValue(
        val category: String,
        val name: String,
        val color: Int,
        val visible: Boolean,
        val customerServiceOnly: Boolean,
        val type: DigitalType
    )

    enum class DigitalType { INPUT, OUTPUT }

    data class EnumField(
        override val index: Int,
        val name: String,
        val values: List<String>
    ) : ReadableField {
        override fun readValue(byteBuffer: ByteBuffer): List<Double> {
            // Not sure what to do with the value, in a test file it is always 0
            // (is it the enum ordinal?)
            return listOf(byteBuffer.short.toUShort().toInt().toDouble())
        }
    }

    data class DataSet(
        val timestampEpochSecond: Long,
        val fieldValues: List<List<Double>>
    ) {
        fun getValue(field: AnalogueField): Double = fieldValues[field.index][0]
        fun getValue(field: DigitalField): List<Double> = fieldValues[field.index]
    }
}