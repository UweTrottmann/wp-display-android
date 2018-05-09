/*
 * Copyright 2018 Uwe Trottmann
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

package com.uwetrottmann.wpdisplay.display

import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.model.*
import java.text.DateFormat

class DisplayAdapter(private val itemIds: MutableList<Int>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var statusData: StatusData = StatusData(IntArray(StatusData.LENGTH_BYTES))
    private var connectionStatus: ConnectionStatus = ConnectionStatus("", false)

    fun updateStatus(newConnectionStatus: ConnectionStatus) {
        connectionStatus = newConnectionStatus
        notifyItemChanged(0)
    }

    fun setItems(newItemIds: List<Int>) {
        itemIds.clear()
        itemIds.addAll(newItemIds)
        notifyDataSetChanged() // TODO use diff helper
    }

    fun updateStatusData(newStatusData: StatusData) {
        statusData = newStatusData
        notifyItemRangeChanged(0, itemCount)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            VIEW_TYPE_HEADER
        } else {
            val itemId = itemIds[position - 1]
            when (DisplayItems.getOrThrow(itemId)) {
                is TemperatureItem -> VIEW_TYPE_TEMPERATURE
                is DurationItem -> VIEW_TYPE_DURATION
                is TextItem -> VIEW_TYPE_TEXT
                else -> throw IllegalArgumentException("View type unknown for position $position")
            }
        }
    }

    override fun getItemCount() = 1 /* header */ + itemIds.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_status, parent, false)
                return StatusViewHolder(view)
            }
            VIEW_TYPE_TEMPERATURE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_text, parent, false)
                return TemperatureViewHolder(view as TextView)
            }
            VIEW_TYPE_DURATION -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_text, parent, false)
                return DurationViewHolder(view as TextView)
            }
            VIEW_TYPE_TEXT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_text, parent, false)
                return TextViewHolder(view as TextView)
            }
            else -> throw IllegalArgumentException("View type $viewType not supported")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is StatusViewHolder -> {
                holder.textViewDisplayStatus.text = connectionStatus.message
                TextViewCompat.setTextAppearance(holder.textViewDisplayStatus,
                        if (connectionStatus.isWarning)
                            R.style.TextAppearance_App_Body1_Orange
                        else
                            R.style.TextAppearance_App_Body1_Green)
                holder.textViewDisplayTime.text = DateFormat.getDateTimeInstance().format(statusData.timestamp)
            }
            is TemperatureViewHolder -> {
                val temperatureItem = DisplayItems.getOrThrow(itemIds[position - 1]) as TemperatureItem
                temperatureItem.setTemperature(holder.textView, statusData)
            }
            is DurationViewHolder -> {
                val durationItem = DisplayItems.getOrThrow(itemIds[position - 1]) as DurationItem
                durationItem.setDuration(holder.textView, statusData)
            }
            is TextViewHolder -> {
                val textItem = DisplayItems.getOrThrow(itemIds[position - 1]) as TextItem
                textItem.setText(holder.textView, statusData)
            }
        }
    }

    class StatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewDisplayStatus = itemView.findViewById<TextView>(R.id.textViewDisplayStatus)
        val textViewDisplayTime = itemView.findViewById<TextView>(R.id.textViewDisplayTime)
    }

    class TemperatureViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    class DurationViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    class TextViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_TEMPERATURE = 1
        const val VIEW_TYPE_DURATION = 2
        const val VIEW_TYPE_TEXT = 3
    }

}