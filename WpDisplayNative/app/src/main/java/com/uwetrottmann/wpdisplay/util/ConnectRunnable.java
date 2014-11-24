package com.uwetrottmann.wpdisplay.util;

import android.util.Log;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.net.Socket;

class ConnectRunnable implements Runnable {

    public interface ConnectListener {
        Socket getSocket();

        void setSocket(Socket socket);
    }

    private final ConnectListener listener;

    public ConnectRunnable(ConnectListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.listener = listener;
    }

    @Override
    public void run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Socket socket = listener.getSocket();
        if (socket != null && socket.isConnected()) {
            Log.i(ConnectionTools.TAG, "connect: already connected");
        }
        Log.i(ConnectionTools.TAG, "connect");

        try {
            socket = new Socket("192.168.178.51", 8888);
            listener.setSocket(socket);
        } catch (IOException e) {
            Log.e(ConnectionTools.TAG, e.getMessage());
        }

        EventBus.getDefault().postSticky(new ConnectionTools.ConnectionEvent(true));
    }
}
