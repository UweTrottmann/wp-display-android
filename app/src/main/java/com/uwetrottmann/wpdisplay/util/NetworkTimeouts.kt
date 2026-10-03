// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.util

/**
 * Timeouts used for connections to the heat pump controller.
 */
object NetworkTimeouts {

    /** Timeout for establishing a connection to the controller. */
    const val CONNECT_TIMEOUT_MS = 15 * 1000 // 15 sec

    /** Timeout for a read from the controller to return data. */
    const val READ_TIMEOUT_MS = 20 * 1000 // 20 sec

}
