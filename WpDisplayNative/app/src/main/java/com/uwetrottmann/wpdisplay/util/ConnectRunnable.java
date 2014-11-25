package com.uwetrottmann.wpdisplay.util;

import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import timber.log.Timber;

class ConnectRunnable implements Runnable {

    public interface ConnectListener {
        Socket getSocket();

        InputStream getInputStream();

        OutputStream getOutputStream();

        void setSocket(Socket socket, InputStream in, OutputStream out);
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
            Timber.d("run: already connected");
        }
        Timber.d("run: connecting");

        String host = "192.168.178.51";
        int port = 8888;

        try {
            // connect, create in and out streams
            socket = new Socket(host, port);
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
