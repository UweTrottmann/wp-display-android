package com.uwetrottmann.wpdisplay.util;

import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.net.Socket;
import timber.log.Timber;

class ConnectRunnable implements Runnable {

    private final ConnectionListener listener;
    private final String host;
    private final int port;

    public ConnectRunnable(ConnectionListener listener, String host, int port) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.listener = listener;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Socket socket = listener.getSocket();
        if (socket != null && socket.isConnected()) {
            Timber.d("run: already connected");
        }
        Timber.d("run: connecting");

        try {
            // connect, create in and out streams
            socket = new Socket(host, port);
            socket.setSoTimeout(15 * 1000); // 15 sec
            listener.setSocket(socket, socket.getInputStream(), socket.getOutputStream());

            // post success
            EventBus.getDefault().postSticky(new ConnectionTools.ConnectionEvent(true));
            return;
        } catch (IOException e) {
            Timber.e(e, "run: connecting to " + host + ":" + port + " failed");
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        // post failure
        EventBus.getDefault().postSticky(new ConnectionTools.ConnectionEvent(false));
    }
}
