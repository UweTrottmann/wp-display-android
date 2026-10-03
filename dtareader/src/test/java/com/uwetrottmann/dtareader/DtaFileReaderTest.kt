// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2022 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.dtareader

import okio.Buffer
import org.junit.Test
import java.io.File
import java.io.IOException
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
        val lastField = readLoggerFile.analogueFields.last()
        assertEquals("Text_WP_Typ", lastField.name)

        // Check fields used by the app exist
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "TVL" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "TRL" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "TA" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "TBW" })

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
        val lastField = readLoggerFile.analogueFields.last()
        assertEquals("Text_WP_Typ", lastField.name)

        // Check fields used by the app exist
        // Note this file uses different names
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "Text_Vorlauf" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "Text_Rucklauf" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "Text_Aussent" })
        assertNotNull(readLoggerFile.analogueFields.find { it.name == "Text_BW_Ist" })

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

}