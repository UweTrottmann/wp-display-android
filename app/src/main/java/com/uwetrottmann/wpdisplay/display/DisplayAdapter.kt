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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.databinding.ItemTextBinding
import com.uwetrottmann.wpdisplay.databinding.LayoutStatusBinding
import com.uwetrottmann.wpdisplay.model.ConnectionStatus
import com.uwetrottmann.wpdisplay.model.DisplayItem
import com.uwetrottmann.wpdisplay.model.HalfWidthItem
import com.uwetrottmann.wpdisplay.model.TemperatureItem
import com.uwetrottmann.wpdisplay.model.FullWidthItem
import com.uwetrottmann.wpdisplay.util.copyTextToClipboardOnClick

class DisplayAdapter(private val displayItems: MutableList<DisplayItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var connectionStatus: ConnectionStatus = ConnectionStatus("", false)
    private var timestamp = ""

    fun updateStatus(newConnectionStatus: ConnectionStatus) {
        connectionStatus = newConnectionStatus
        notifyItemChanged(0)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDisplayItems(newTimestamp: String, newDisplayItems: List<DisplayItem>) {
        timestamp = newTimestamp
        displayItems.clear()
        displayItems.addAll(newDisplayItems)
        notifyDataSetChanged() // TODO use diff helper
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            VIEW_TYPE_HEADER
        } else {
            when (displayItems[position - 1]) {
                is TemperatureItem -> VIEW_TYPE_TEMPERATURE
                is HalfWidthItem -> VIEW_TYPE_DURATION
                is FullWidthItem -> VIEW_TYPE_TEXT
                else -> throw IllegalArgumentException("View type unknown for position $position")
            }
        }
    }

    override fun getItemCount() = OFFSET /* header */ + displayItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding =
                    LayoutStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return StatusViewHolder(binding)
            }
            VIEW_TYPE_TEMPERATURE -> {
                val binding =
                    ItemTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return TemperatureViewHolder(binding)
            }
            VIEW_TYPE_DURATION -> {
                val binding =
                    ItemTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return DurationViewHolder(binding)
            }
            VIEW_TYPE_TEXT -> {
                val binding =
                    ItemTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return TextViewHolder(binding)
            }
            else -> throw IllegalArgumentException("View type $viewType not supported")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is StatusViewHolder -> {
                holder.binding.textViewDisplayStatus.text = connectionStatus.message
                TextViewCompat.setTextAppearance(
                    holder.binding.textViewDisplayStatus,
                    if (connectionStatus.isWarning)
                        R.style.TextAppearance_App_Body1_Error
                    else
                        R.style.TextAppearance_App_Body1_Green
                )
                holder.binding.textViewDisplayTime.text = timestamp
            }
            is TemperatureViewHolder -> {
                holder.binding.textView.text = displayItems[position - OFFSET].charSequence
            }
            is DurationViewHolder -> {
                holder.binding.textView.text = displayItems[position - OFFSET].charSequence
            }
            is TextViewHolder -> {
                holder.binding.textView.text = displayItems[position - OFFSET].charSequence
            }
        }
    }

    class StatusViewHolder(val binding: LayoutStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.textViewDisplayStatus.copyTextToClipboardOnClick()
            binding.textViewDisplayTime.copyTextToClipboardOnClick()
        }
    }

    class TemperatureViewHolder(val binding: ItemTextBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.textView.copyTextToClipboardOnClick()
        }
    }

    class DurationViewHolder(val binding: ItemTextBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.textView.copyTextToClipboardOnClick()
        }
    }

    class TextViewHolder(val binding: ItemTextBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.textView.copyTextToClipboardOnClick()
        }
    }

    companion object {
        const val OFFSET = 1

        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_TEMPERATURE = 1
        const val VIEW_TYPE_DURATION = 2
        const val VIEW_TYPE_TEXT = 3
    }

}