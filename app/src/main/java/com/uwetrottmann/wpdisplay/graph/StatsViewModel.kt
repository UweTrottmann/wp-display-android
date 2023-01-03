/*
 * Copyright 2023 Uwe Trottmann
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

package com.uwetrottmann.wpdisplay.graph

import android.app.Application
import android.content.Context
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.uwetrottmann.dtareader.DtaFileReader
import com.uwetrottmann.wpdisplay.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class StatsViewModel(
    private val host: String?,
    application: Application
) : AndroidViewModel(application) {

    data class Result(
        val errorMessage: String?,
        val chartData: LineData?
    )

    val chartData = MutableLiveData<Result>()

    init {
        loadChartData()
    }

    private fun loadChartData() {
        val context = getApplication<Application>().applicationContext

        if (host.isNullOrEmpty()) {
            chartData.postValue(Result(context.getString(R.string.setup_missing), null))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val reader = DtaFileReader()
            try {
                val input = reader.getLoggerFileStream(host)
                val dtaFile = reader.readLoggerFile(input)
                val lineData = buildLineData(context, dtaFile)
                chartData.postValue(Result(null, lineData))
            } catch (e: IOException) {
                chartData.postValue(
                    Result(
                        context.getString(
                            R.string.stats_error_load,
                            "${e::class.simpleName} ${e.message}"
                        ), null
                    )
                )
            }
        }
    }

    data class FieldToDisplay(
        val field: DtaFileReader.AnalogueField,
        val label: String,
        val color: Int,
        val entries: MutableList<Entry> = mutableListOf()
    )

    private fun buildLineData(context: Context, dtaFile: DtaFileReader.DtaFile): LineData {
        val analogueFields = dtaFile.analogueFields

        val fieldsToDisplay = mutableListOf<FieldToDisplay>()
        analogueFields.find { it.name == "TVL" }
            ?.let { FieldToDisplay(it, context.getString(R.string.label_temp_outgoing), Color.RED) }
            ?.let { fieldsToDisplay.add(it) }
        analogueFields.find { it.name == "TRL" }
            ?.let { FieldToDisplay(it, context.getString(R.string.label_temp_return), Color.BLUE) }
            ?.let { fieldsToDisplay.add(it) }
        analogueFields.find { it.name == "TA" }
            ?.let {
                FieldToDisplay(it, context.getString(R.string.label_temp_outdoors), Color.MAGENTA)
            }
            ?.let { fieldsToDisplay.add(it) }
        analogueFields.find { it.name == "TBW" }
            ?.let { FieldToDisplay(it, context.getString(R.string.label_temp_water), Color.CYAN) }
            ?.let { fieldsToDisplay.add(it) }

        dtaFile.datasets.forEach { dataset ->
            val timestamp = dataset.timestampEpochSecond
            fieldsToDisplay.forEach {
                it.entries.add(Entry(timestamp.toFloat(), dataset.getValue(it.field).toFloat()))
            }
        }

        return fieldsToDisplay
            .map {
                LineDataSet(it.entries, it.label)
                    .apply {
                        color = it.color
                        setCircleColor(it.color)
                    }
            }
            .let { LineData(it) }
    }

    class Factory(
        private val host: String?,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(host, application) as T
        }
    }

}