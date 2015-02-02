/*
 * Copyright 2015 Uwe Trottmann
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

package com.uwetrottmann.wpdisplay.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import timber.log.Timber;

public class DisconnectRunnable implements Runnable {

    private final ConnectionListener listener;

    public DisconnectRunnable(ConnectionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.listener = listener;
    }

    @Override
    public void run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Timber.d("run: disconnecting");

        try {
            InputStream in = listener.getInputStream();
            if (in != null) {
                in.close();
            }

            OutputStream out = listener.getOutputStream();
            if (out != null) {
                out.close();
            }

            Socket socket = listener.getSocket();
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Timber.e(e, "run: disconnecting failed");
        }

        listener.setSocket(null, null, null);
    }
}
