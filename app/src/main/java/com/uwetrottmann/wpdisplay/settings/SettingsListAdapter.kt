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

package com.uwetrottmann.wpdisplay.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uwetrottmann.wpdisplay.databinding.ItemSelectableBinding
import com.uwetrottmann.wpdisplay.model.DisplayItem

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
                setText(item.type.labelResId)
            }
        }
    }

}