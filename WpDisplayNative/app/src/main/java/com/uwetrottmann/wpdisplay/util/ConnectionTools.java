package com.uwetrottmann.wpdisplay.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionTools implements ConnectRunnable.ConnectListener {

    public static class ConnectionEvent {
        public boolean isConnected;

        public ConnectionEvent(boolean isConnected) {
            this.isConnected = isConnected;
        }
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    private final ConnectRunnable connectRunnable;
    private final DisconnectRunnable disconnectRunnable;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

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
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public synchronized void setSocket(Socket socket, InputStream in, OutputStream out) {
        this.socket = socket;
        this.inputStream = new DataInputStream(in);
        this.outputStream = new DataOutputStream(out);
    }
}
