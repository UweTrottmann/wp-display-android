// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2018 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.settings

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.uwetrottmann.wpdisplay.model.DisplayItem
import com.uwetrottmann.wpdisplay.model.DisplayItems
import kotlinx.coroutines.Dispatchers

@SuppressLint("StaticFieldLeak")
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val availableItems: LiveData<List<DisplayItem>> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            DisplayItems.readDisabledStateFromPreferences(application)
            emit(DisplayItems.all) // no copy
        }

}
