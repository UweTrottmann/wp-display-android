package com.uwetrottmann.wpdisplay.util;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionTools implements ConnectRunnable.ConnectListener {

    public static final String TAG = "ConnectionTools";

    public static class ConnectionEvent {
        public boolean isConnected;

        public ConnectionEvent(boolean isConnected) {
            this.isConnected = isConnected;
        }
    }

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    private final ConnectRunnable connectRunnable;
    private final DisconnectRunnable disconnectRunnable;
    private Socket socket;

    public ConnectionTools() {
        connectRunnable = new ConnectRunnable(this);
        disconnectRunnable = new DisconnectRunnable(this);
    }

    public void connect() {
        executor.execute(connectRunnable);
    }

    public void disconnect() {
        executor.execute(disconnectRunnable);
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    @Override
    public synchronized void setSocket(Socket socket) {
        this.socket = socket;
    }
}
