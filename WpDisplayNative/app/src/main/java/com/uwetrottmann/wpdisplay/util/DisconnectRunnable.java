package com.uwetrottmann.wpdisplay.util;

import android.util.Log;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class DisconnectRunnable implements Runnable {

    private static final String TAG = "DisconnectRunnable";

    private final ConnectRunnable.ConnectListener listener;

    public DisconnectRunnable(ConnectRunnable.ConnectListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.listener = listener;
    }

    @Override
    public void run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Log.i(TAG, "run: disconnecting");

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
            Log.e(ConnectionTools.TAG, e.getMessage());
        }

        listener.setSocket(null, null, null);

        EventBus.getDefault().postSticky(new ConnectionTools.ConnectionEvent(false));
    }
}
