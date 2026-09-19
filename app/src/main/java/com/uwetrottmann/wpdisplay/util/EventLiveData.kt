// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2018 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.util

import androidx.lifecycle.MutableLiveData

class EventLiveData<T> : MutableLiveData<Event<T>>() {

    fun postEvent(value: T) {
        super.postValue(Event(value))
    }

}