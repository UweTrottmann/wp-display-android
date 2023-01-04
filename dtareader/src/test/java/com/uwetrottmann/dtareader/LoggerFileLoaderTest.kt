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
import java.time.Instant
import kotlin.test.assertEquals

class LoggerFileLoaderTest {

    @Test
    fun getAndReadLoggerFileStream() {
        val testFileInputStream = File("src/test/testdata/NewProc-2022-04-02.dta").inputStream()

        val loader = DtaFileReader()
        val readLoggerFile = loader.readLoggerFile(testFileInputStream)
        readLoggerFile.fields.forEach { println(it) }
        assertEquals(22, readLoggerFile.fields.size)
        assertEquals(20, readLoggerFile.analogueFields.size)
        assertEquals(2, readLoggerFile.digitalFields.size)

        assertEquals(2880, readLoggerFile.datasets.size)
        readLoggerFile.datasets.forEach {
            assertEquals(20 + 2, it.fieldValues.size)
            println("${Instant.ofEpochSecond(it.timestampEpochSecond)} ${it.fieldValues}")
        }
    }

}