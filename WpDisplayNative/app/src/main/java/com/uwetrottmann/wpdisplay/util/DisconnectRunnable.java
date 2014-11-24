package com.uwetrottmann.wpdisplay.util;

import android.util.Log;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.net.Socket;

public class DisconnectRunnable implements Runnable {

    private final ConnectRunnable.ConnectListener listener;

    public DisconnectRunnable(ConnectRunnable.ConnectListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.listener = listener;
    }

    @Override
    public void run() {
        Socket socket = listener.getSocket();
        if (socket == null) {
            Log.i(ConnectionTools.TAG, "disconnect: socket is null");
            return;
        }
        Log.i(ConnectionTools.TAG, "disconnect");

        try {
            socket.close();
        } catch (IOException e) {
            Log.e(ConnectionTools.TAG, e.getMessage());
        }

        listener.setSocket(null);

        EventBus.getDefault().postSticky(new ConnectionTools.ConnectionEvent(false));
    }
}
