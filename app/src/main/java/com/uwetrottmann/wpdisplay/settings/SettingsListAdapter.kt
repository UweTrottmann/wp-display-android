// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2018 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uwetrottmann.wpdisplay.databinding.ItemSelectableBinding
import com.uwetrottmann.wpdisplay.model.DisplayItem
import com.uwetrottmann.wpdisplay.model.StatusData
import com.uwetrottmann.wpdisplay.util.DataRequestRunnable

class SettingsListAdapter :
    ListAdapter<DisplayItem, SettingsListAdapter.SettingsViewHolder>(object :
        DiffUtil.ItemCallback<DisplayItem>() {
        override fun areItemsTheSame(oldItem: DisplayItem, newItem: DisplayItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DisplayItem, newItem: DisplayItem): Boolean =
            oldItem.enabled == newItem.enabled
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val binding =
            ItemSelectableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    class SettingsViewHolder(private val binding: ItemSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindTo(item: DisplayItem) {
            binding.checkBoxItemSelectable.apply {
                setOnCheckedChangeListener(null) // disable while binding
                isChecked = item.enabled
                setOnCheckedChangeListener { _, isChecked -> item.enabled = isChecked }
                val currentData = DataRequestRunnable.statusData.value ?: StatusData()
                text = currentData.getLabelFor(item.type, this.context)
            }
        }
    }

}