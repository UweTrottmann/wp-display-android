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

        val firstField = readLoggerFile.analogueFields.first()
        assertEquals("TVL", firstField.name)
        val lastField = readLoggerFile.analogueFields.last()
        assertEquals("Text_WP_Typ", lastField.name)

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

}