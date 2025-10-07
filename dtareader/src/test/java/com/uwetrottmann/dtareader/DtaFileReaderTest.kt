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

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DtaFileReaderTest {

    @Test
    fun getAndReadLoggerFileStream() {
//        val testFileInputStream = File("src/test/testdata/NewProc-2022-04-02.dta").inputStream()
        val testFileInputStream = File("src/test/testdata/NewProc-only-one-hour.dta").inputStream()

        val loader = DtaFileReader()
        val readLoggerFile = loader.readLoggerFile(testFileInputStream)

        readLoggerFile.fields.forEach { println(it) }
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

}