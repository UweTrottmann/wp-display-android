// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2023 Uwe Trottmann <uwe@uwetrottmann.com>

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
import com.uwetrottmann.wpdisplay.util.NetworkTimeouts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class StatsViewModel(
    private val host: String?,
    application: Application
) : AndroidViewModel(application) {

    /**
     * The x values of [chartData] are seconds relative to [timestampBaseEpochSecond]. See notes in
     * [buildResult].
     */
    data class Result(
        val errorMessage: String?,
        val chartData: LineData?,
        val timestampBaseEpochSecond: Long = 0
    )

    val chartData = MutableLiveData<Result>()

    init {
        loadChartData()
    }

    fun loadChartData() {
        val context = getApplication<Application>().applicationContext

        if (host.isNullOrEmpty()) {
            chartData.postValue(Result(context.getString(R.string.setup_missing), null))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val reader = DtaFileReader()
            try {
//                val input = context.resources.assets.open("NewProc-Test.dta")
                val input = reader.getLoggerFileStream(
                    host,
                    NetworkTimeouts.CONNECT_TIMEOUT_MS,
                    NetworkTimeouts.READ_TIMEOUT_MS
                )
                val dtaFile = reader.readLoggerFile(input)
                chartData.postValue(buildResult(context, dtaFile))
            } catch (e: Exception) {
                Timber.e(e, "Failed to read logger file")
                val cause: String = e.cause?.let { " cause: ${it.toMessage()}" } ?: ""
                chartData.postValue(
                    Result(
                        context.getString(
                            R.string.stats_error_load,
                            "${e.toMessage()}$cause"
                        ), null
                    )
                )
            }
        }
    }

    private fun Throwable.toMessage(): String {
        return "${this::class.simpleName}: ${this.message}"
    }

    data class FieldToDisplay(
        val field: DtaFileReader.AnalogueField,
        val label: String,
        val color: Int,
        val entries: MutableList<Entry> = mutableListOf()
    )

    private fun buildResult(context: Context, dtaFile: DtaFileReader.DtaFile): Result {
        val analogueFields = dtaFile.analogueFields

        val fieldsToDisplay = mutableListOf<FieldToDisplay>()
        analogueFields.find { it.name == "TVL" || it.name == "Text_Vorlauf" }
            ?.let { FieldToDisplay(it, context.getString(R.string.label_temp_outgoing), Color.RED) }
            ?.let { fieldsToDisplay.add(it) }
        analogueFields.find { it.name == "TRL" || it.name == "Text_Rucklauf" }
            ?.let { FieldToDisplay(it, context.getString(R.string.label_temp_return), Color.BLUE) }
            ?.let { fieldsToDisplay.add(it) }
        analogueFields.find { it.name == "TA" || it.name == "Text_Aussent" }
            ?.let {
                FieldToDisplay(it, context.getString(R.string.label_temp_outdoors), Color.MAGENTA)
            }
            ?.let { fieldsToDisplay.add(it) }
        analogueFields.find { it.name == "TBW" || it.name == "Text_BW_Ist" }
            ?.let { FieldToDisplay(it, context.getString(R.string.label_temp_water), Color.CYAN) }
            ?.let { fieldsToDisplay.add(it) }

        // The chart requires Float coordinates. But a Float can not represent current epoch seconds
        // precisely enough (current values of 1.7e9 only in steps of 128 seconds).
        // So shift the x values so the first timestamp is at x-value zero. This is enough precision
        // for 48 hours of values.
        // The "downside" is that the chart must adjust its x-axis description accordingly.
        val timestampBase = dtaFile.datasets.firstOrNull()?.timestampEpochSecond ?: 0
        dtaFile.datasets.forEach { dataset ->
            val timestampRelative = dataset.timestampEpochSecond - timestampBase
            fieldsToDisplay.forEach {
                it.entries.add(
                    Entry(timestampRelative.toFloat(), dataset.getValue(it.field).toFloat())
                )
            }
        }

        val lineData = fieldsToDisplay
            .map {
                LineDataSet(it.entries, it.label)
                    .apply {
                        color = it.color
                        setCircleColor(it.color)
                    }
            }
            .let { LineData(it) }

        return Result(null, lineData, timestampBase)
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