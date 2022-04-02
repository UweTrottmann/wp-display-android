/*
 * Copyright 2022 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uwetrottmann.dtareader

import okio.BufferedSource
import okio.IOException
import okio.buffer
import okio.source
import java.io.InputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and

/**
 * Based upon https://sourceforge.net/p/opendta/git/ci/master/tree/dtafile/dtafile9003.cpp
 */
class DtaFileReader {

    fun getLoggerFileStream(host: String): InputStream? {
        val url = URL("http://$host/NewProc")
        return url.openConnection().getInputStream()
    }

    fun readLoggerFile(inputStream: InputStream): DtaFile {
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
    }

    private data class Header(
        val fields: List<ReadableField>,
        val datasetsToRead: Short,
        val datasetLength: Short
    )

    private fun parseHeader(bufferedSource: BufferedSource): Header {
        // Byte [4:7]: size of header
        val headerSize = bufferedSource.readIntLe()
        if (!bufferedSource.request(headerSize.toLong())) {
            throw IOException("Header is not $headerSize bytes long")
        }
        val headerBytes = bufferedSource.readByteArray(headerSize.toLong())
        val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Byte [8:9]: number of data sets
        val datasetsToRead = headerBuffer.short
        // Byte [10:11]: length of a data set
        val datasetLength = headerBuffer.short
        if (datasetLength < 6) {
            throw IOException("Data set length is smaller than 6 bytes (at least timestamp + 2 byte field)")
        }

        val fields = mutableListOf<ReadableField>()
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
                        headerBuffer.short
                    } else 10
                    fields.add(AnalogueField(category, name, color, factor))
                }
                0x02.toByte(), 0x04.toByte() -> {
                    // Digital field
                    val count = headerBuffer.get()
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
                    fields.add(DigitalField(values))
                }
                0x03.toByte() -> {
                    // TODO Appears not used in test file, so not implementing.
                    //   Ask users to send in their file to add to this project for testing.
                    throw IOException("Enum fields are not supported, please send your DTA file for testing.")
//                            // Enum field
//                            val name = readString(headerBuffer)
//                            val count = headerBuffer.get()
//
//                            val enumValues = mutableListOf<String>()
//                            for (i in 0 until count) {
//                                val enumValue = readString(headerBuffer)
//                                enumValues.add(enumValue)
//                            }
                }
                else -> throw IOException("Unknown field type $fieldType")
            }
        }

        // Check number of fields * 2 (length of value) == data set length
        val expectedDataSetLength = fields.size * 2 + 4 // 4 byte time stamp
        if (expectedDataSetLength != datasetLength.toInt()) {
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

            // First 4 bytes are unix time in seconds
            val epochSecond = buffer.int
            // Then for each field 2 bytes
            val fieldValues = mutableListOf<List<Double>>()
            header.fields.forEach { fieldValues.add(it.readValue(buffer)) }

            datasets.add(
                DataSet(
                    epochSecond.toLong(),
                    fieldValues
                )
            )
        }
        return datasets
    }

    private fun readString(buffer: ByteBuffer): String {
        var string = ""
        while (true) {
            val char = buffer.get()
            if (char == 0x0.toByte()) {
                break
            } else {
                string += char.toInt().toChar()
            }
        }
        return string
    }

    private fun readColor(buffer: ByteBuffer): Int {
        val r = buffer.get().toLong()
        val g = buffer.get().toLong()
        val b = buffer.get().toLong()
        return (0xFF000000 or r shl 16 or g shl 8 or b).toInt()
    }

    companion object {
        const val VERSION_9003 = 9003
    }

    data class DtaFile(
        val version: Int,
        val fields: List<ReadableField>,
        val datasets: List<DataSet>
    ) {
        val analogueFields: List<AnalogueField> = fields.filterIsInstance<AnalogueField>()
        val digitalFields: List<DigitalField> = fields.filterIsInstance<DigitalField>()
    }

    interface ReadableField {
        fun readValue(byteBuffer: ByteBuffer): List<Double>
    }

    data class AnalogueField(
        val category: String,
        val name: String,
        val color: Int,
        val factor: Short
    ) : ReadableField {
        override fun readValue(byteBuffer: ByteBuffer): List<Double> {
            val value = byteBuffer.short
            return listOf(value.toDouble() / factor)
        }
    }

    data class DigitalField(
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

    data class DataSet(
        val timestampEpochSecond: Long,
        val fieldValues: List<List<Double>>
    )
}