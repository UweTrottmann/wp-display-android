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
