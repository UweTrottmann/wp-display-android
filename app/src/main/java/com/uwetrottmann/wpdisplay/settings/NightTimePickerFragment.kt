// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2018 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.settings

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.uwetrottmann.wpdisplay.R

/**
 * Dialog allowing to pick a time (hour and minute) for night start or end.
 */
class NightTimePickerFragment : AppCompatDialogFragment() {

    private var changeNightStart: Boolean = true
    private var initialHour: Int = -1
    private var initialMinute: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireArguments().run {
            changeNightStart = getBoolean(ARG_CHANGE_NIGHT_START)
            initialHour = getInt(ARG_INITIAL_HOUR, -1)
            if (initialHour == -1) throw IllegalArgumentException("Missing $ARG_INITIAL_HOUR argument")
            initialMinute = getInt(ARG_INITIAL_MINUTE, -1)
            if (initialMinute == -1) throw IllegalArgumentException("Missing $ARG_INITIAL_MINUTE argument")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return TimePickerDialog(
            context, onTimeSetListener, initialHour, initialMinute,
            DateFormat.is24HourFormat(activity)
        )
    }

    private val onTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
        if (changeNightStart) {
            ThemeSettings.saveNightStart(requireContext(), hourOfDay, minute)
        } else {
            ThemeSettings.saveNightEnd(requireContext(), hourOfDay, minute)
        }
        // post so dialog has time to close (otherwise is re-shown)
        Handler(Looper.getMainLooper()).post {
            (parentFragmentManager.findFragmentById(R.id.container) as? SettingsFragment)?.run {
                populateViews()
                updateColorScheme()
            }
        }
    }

    companion object {

        private const val ARG_CHANGE_NIGHT_START = "change.night.start"
        private const val ARG_INITIAL_HOUR = "default.hour"
        private const val ARG_INITIAL_MINUTE = "default.minute"

        fun showIfSafe(
            fragmentManager: FragmentManager, changeNightStart: Boolean,
            initialHour: Int, initialMinute: Int
        ) {
            if (fragmentManager.isStateSaved) return

            NightTimePickerFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_CHANGE_NIGHT_START, changeNightStart)
                    putInt(ARG_INITIAL_HOUR, initialHour)
                    putInt(ARG_INITIAL_MINUTE, initialMinute)
                }
            }.show(fragmentManager, "timePickerDialog")
        }

    }

}