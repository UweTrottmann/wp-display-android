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

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.model.DisplayItem

class SettingsListAdapter : ListAdapter<DisplayItem, SettingsListAdapter.SettingsViewHolder>(object : DiffUtil.ItemCallback<DisplayItem>() {
    override fun areItemsTheSame(oldItem: DisplayItem?, newItem: DisplayItem?): Boolean =
            oldItem?.id == newItem?.id

    override fun areContentsTheSame(oldItem: DisplayItem?, newItem: DisplayItem?): Boolean =
            oldItem?.enabled == newItem?.enabled
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selectable, parent, false)
        return SettingsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    class SettingsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox = view.findViewById<CheckBox>(R.id.checkBoxItemSelectable)
        fun bindTo(item: DisplayItem) {
            checkBox.isChecked = item.enabled
            checkBox.setText(item.labelResId)
        }
    }

}