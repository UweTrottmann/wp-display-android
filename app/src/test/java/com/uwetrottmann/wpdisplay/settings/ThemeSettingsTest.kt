// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.settings

import com.uwetrottmann.wpdisplay.settings.ThemeSettings.isNightTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSettingsTest {

    private fun minutes(hour: Int, minute: Int) = hour * 60 + minute

    @Test
    fun isNightTime_spansMidnight() {
        val start = minutes(21, 0)
        val end = minutes(7, 0)

        // 1 minute before start
        assertFalse(isNightTime(minutes(20, 59), start, end))
        // start is inclusive
        assertTrue(isNightTime(now = start, start, end))

        // before and at midnight
        assertTrue(isNightTime(minutes(23, 59), start, end))
        assertTrue(isNightTime(minutes(0, 0), start, end))

        // 1 minute before end
        assertTrue(isNightTime(minutes(6, 59), start, end))
        // end is exclusive
        assertFalse(isNightTime(now = end, start, end))

        // midday
        assertFalse(isNightTime(minutes(12, 0), start, end))
    }

    @Test
    fun isNightTime_withinSameDay() {
        val start = minutes(1, 0)
        val end = minutes(6, 0)

        // 1 minute before start
        assertFalse(isNightTime(minutes(0, 59), start, end))
        // start is inclusive
        assertTrue(isNightTime(now = start, start, end))

        assertTrue(isNightTime(minutes(3, 30), start, end))

        // 1 minute before end
        assertTrue(isNightTime(minutes(5, 59), start, end))
        // end is exclusive
        assertFalse(isNightTime(now = end, start, end))

        // midday
        assertFalse(isNightTime(minutes(12, 0), start, end))

        assertFalse(isNightTime(minutes(23, 0), start, end))
    }

    @Test
    fun isNightTime_minutesInSameHour() {
        // Night within a single hour, checks minutes are not ignored
        val start = minutes(21, 15)
        val end = minutes(21, 45)

        // 1 minute before start
        assertFalse(isNightTime(minutes(21, 14), start, end))
        // start is inclusive
        assertTrue(isNightTime(now = start, start, end))
        // 1 minute before end
        assertTrue(isNightTime(minutes(21, 44), start, end))
        // end is exclusive
        assertFalse(isNightTime(now = end, start, end))
    }

    @Test
    fun isNightTime_startEqualsEnd_neverNight() {
        val time = minutes(7, 0)

        assertFalse(isNightTime(now = time, time, time))
        // midnight
        assertFalse(isNightTime(minutes(0, 0), time, time))
        // midday
        assertFalse(isNightTime(minutes(12, 0), time, time))
        // with minutes
        assertFalse(isNightTime(minutes(23, 59), time, time))
    }

}
