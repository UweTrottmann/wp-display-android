// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2022 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.dtareader

import okio.Buffer
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class DtaFileReaderTest {

    @Test
    fun getAndReadLoggerFileStream() {
        val testFileInputStream = File("src/test/testdata/NewProc-2022-04-02.dta").inputStream()

        val loader = DtaFileReader()
        val readLoggerFile = loader.readLoggerFile(testFileInputStream)

//        readLoggerFile.fields.forEach { println(it) }
        assertEquals(22, readLoggerFile.fields.size)
        assertEquals(20, readLoggerFile.analogueFields.size)
        assertEquals(2, readLoggerFile.digitalFields.size)
        assertEquals(0, readLoggerFile.enumFields.size)

        val firstField = readLoggerFile.analogueFields.first()
        assertEquals("TVL", firstField.name)
        assertEquals(0xFFFF0000.toInt(), firstField.color) // red
        val lastField = readLoggerFile.analogueFields.last()
        assertEquals("Text_WP_Typ", lastField.name)
        assertEquals(0xFF0000FF.toInt(), lastField.color) // blue

        // Check fields used by the app exist
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "TVL" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "TRL" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "TA" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "TBW" })

        // Colors (not used by the app, but still verify; compare with OpenDTA!)
        assertAnalogueColor(readLoggerFile, "TRL", 0xFF0000FF) // blue
        assertAnalogueColor(readLoggerFile, "TA", 0xFF808000) // olive
        assertAnalogueColor(readLoggerFile, "TBW", 0xFF800080) // purple
        readLoggerFile.digitalFields.first().values.first().let {
            assertEquals("HDin", it.name)
            assertEquals(0xFFFF0000.toInt(), it.color) // red
        }
        readLoggerFile.digitalFields.last().values.last().let {
            assertEquals("ZW1out", it.name)
            assertEquals(0xFF00FFFF.toInt(), it.color) // cyan
        }

        assertEquals(2880, readLoggerFile.datasets.size)
        readLoggerFile.datasets.forEach { dataSet ->
            assertEquals(20 + 2, dataSet.fieldValues.size)
//            println("${Instant.ofEpochSecond(dataSet.timestampEpochSecond)} ${dataSet.fieldValues}")
            readLoggerFile.analogueFields.forEach {
                dataSet.getValue(it)
            }
            readLoggerFile.digitalFields.forEach {
                dataSet.getValue(it)
            }
        }

        val firstDataSet = readLoggerFile.datasets.first()
        assertEquals(54.9, firstDataSet.getValue(firstField))
        assertEquals(11.0, firstDataSet.getValue(lastField))
        val lastDataSet = readLoggerFile.datasets.last()
        assertEquals(39.1, lastDataSet.getValue(firstField))
        assertEquals(11.0, lastDataSet.getValue(lastField))
    }

    @Test
    fun getAndReadLoggerFileStream_enums() {
        val testFileInputStream = File("src/test/testdata/NewProc-enums.dta").inputStream()

        val loader = DtaFileReader()
        val readLoggerFile = loader.readLoggerFile(testFileInputStream)

        readLoggerFile.fields.forEach { println(it) }
        assertEquals(76, readLoggerFile.fields.size)
        assertEquals(69, readLoggerFile.analogueFields.size)
        assertEquals(6, readLoggerFile.digitalFields.size)
        assertEquals(1, readLoggerFile.enumFields.size)

        val firstField = readLoggerFile.analogueFields.first()
        assertEquals("Text_Vorlauf", firstField.name)
        assertEquals(0xFFFF0000.toInt(), firstField.color) // red
        val lastField = readLoggerFile.analogueFields.last()
        assertEquals("Text_WP_Typ", lastField.name)
        assertEquals(0xFF0000FF.toInt(), lastField.color) // blue

        // Check fields used by the app exist
        // Note this file uses different names
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "Text_Vorlauf" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "Text_Rucklauf" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "Text_Aussent" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "Text_BW_Ist" })

        // Colors (not used by the app, but still verify; compare with OpenDTA!)
        assertAnalogueColor(readLoggerFile, "Text_Rucklauf", 0xFF0000FF) // blue
        assertAnalogueColor(readLoggerFile, "Text_Aussent", 0xFF808000) // olive
        assertAnalogueColor(readLoggerFile, "Text_BW_Ist", 0xFF800080) // purple
        // Color with high bit set in channels
        assertAnalogueColor(readLoggerFile, "Text_MK2VL_Soll", 0xFFA6CAF0)
        readLoggerFile.digitalFields.first().values.first().let {
            assertEquals("Text_EVU", it.name)
            assertEquals(0xFF008080.toInt(), it.color) // teal
        }
        readLoggerFile.digitalFields.last().values.last().let {
            assertEquals("Text_Zwangsbrauchwasser", it.name)
            assertEquals(0xFF800080.toInt(), it.color) // purple
        }

        assertEquals(2880, readLoggerFile.datasets.size)
        readLoggerFile.datasets.forEach { dataSet ->
            assertEquals(69 + 6 + 1, dataSet.fieldValues.size)
//            println("${Instant.ofEpochSecond(dataSet.timestampEpochSecond)} ${dataSet.fieldValues}")
            readLoggerFile.analogueFields.forEach {
                dataSet.getValue(it)
            }
            readLoggerFile.digitalFields.forEach {
                dataSet.getValue(it)
            }
        }

        val firstDataSet = readLoggerFile.datasets.first()
        assertEquals(38.1, firstDataSet.getValue(firstField))
        assertEquals(75.0, firstDataSet.getValue(lastField))
        val lastDataSet = readLoggerFile.datasets.last()
        assertEquals(36.8, lastDataSet.getValue(firstField))
        assertEquals(75.0, lastDataSet.getValue(lastField))
    }

    private fun assertAnalogueColor(dtaFile: DtaFileReader.DtaFile, name: String, color: Long) {
        val field = dtaFile.analogueFields.find { it.name == name }
        assertNotNull(field, "Field $name not found")
        assertEquals(color.toInt(), field.color, "Color of $name")
    }

    /** Version and header size, followed by the given header bytes. */
    private fun fakeDtaStream(headerSize: Int, header: Buffer.() -> Unit = {}) =
        Buffer()
            .writeIntLe(DtaFileReader.VERSION_9003)
            .writeIntLe(headerSize)
            .apply(header)
            .inputStream()

    @Test
    fun readLoggerFile_negativeHeaderSize_throwsIOException() {
        assertFailsWith<IOException> {
            DtaFileReader().readLoggerFile(fakeDtaStream(headerSize = -1))
        }
    }

    @Test
    fun readLoggerFile_headerTooShortForCounts_throwsIOException() {
        assertFailsWith<IOException> {
            DtaFileReader().readLoggerFile(fakeDtaStream(headerSize = 2) {
                writeShortLe(1)
            })
        }
    }

    @Test
    fun readLoggerFile_unterminatedFieldName_throwsIOException() {
        // Analogue field whose name has no 0x00 terminator before the header ends.
        assertFailsWith<IOException> {
            DtaFileReader().readLoggerFile(fakeDtaStream(headerSize = 7) {
                writeShortLe(1) // data sets
                writeShortLe(6) // data set length (passes >= 6 check)
                writeByte(0x01) // analogue field
                writeByte('T'.code)
                writeByte('A'.code) // no terminator, no color
            })
        }
    }

    @Test
    fun readLoggerFile_truncatedColor_throwsIOException() {
        // Name is terminated, but the 3 color bytes are missing.
        assertFailsWith<IOException> {
            DtaFileReader().readLoggerFile(fakeDtaStream(headerSize = 8) {
                writeShortLe(1)
                writeShortLe(6)
                writeByte(0x01)
                writeByte('T'.code)
                writeByte('A'.code)
                writeByte(0x00)
            })
        }
    }

    private fun readColor(vararg bytes: Int): Int =
        DtaFileReader().readColor(ByteBuffer.wrap(ByteArray(bytes.size) { bytes[it].toByte() }))

    @Test
    fun readColor() {
        assertEquals(0xFF123456.toInt(), readColor(0x12, 0x34, 0x56))
        // black and white
        assertEquals(0xFF000000.toInt(), readColor(0x00, 0x00, 0x00))
        assertEquals(0xFFFFFFFF.toInt(), readColor(0xFF, 0xFF, 0xFF))
        // each channel in its place, bytes >= 0x80 must not sign extend
        assertEquals(0xFFFF0000.toInt(), readColor(0xFF, 0x00, 0x00))
        assertEquals(0xFF00FF00.toInt(), readColor(0x00, 0xFF, 0x00))
        assertEquals(0xFF0000FF.toInt(), readColor(0x00, 0x00, 0xFF))
        assertEquals(0xFF80807F.toInt(), readColor(0x80, 0x80, 0x7F))
    }

    @Test
    fun readColor_readsExactly3Bytes() {
        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        DtaFileReader().readColor(buffer)
        assertEquals(3, buffer.position())
    }

    @Test
    fun readColor_tooFewBytes_throws() {
        assertFailsWith<BufferUnderflowException> {
            readColor(0x01, 0x02)
        }
    }

}