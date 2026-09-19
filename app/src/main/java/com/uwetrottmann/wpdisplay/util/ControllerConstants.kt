// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.util

/**
 * Constants used with heat pump controller.
 */
interface ControllerConstants {
    companion object {
        const val COMMAND_REQUEST_SETTINGS = 3003
        const val COMMAND_REQUEST_STATUS = 3004
    }
}