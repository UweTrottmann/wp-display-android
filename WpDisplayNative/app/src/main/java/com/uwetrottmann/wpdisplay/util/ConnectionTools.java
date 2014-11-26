package com.uwetrottmann.wpdisplay.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConnectionTools implements ConnectionListener {

    private static ConnectionTools _instance;

    public static class ConnectionEvent {
        public boolean isConnected;

        public ConnectionEvent(boolean isConnected) {
            this.isConnected = isConnected;
        }
    }

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private final ConnectRunnable connectRunnable;
    private final DisconnectRunnable disconnectRunnable;
    private final DataRequestRunnable requestRunnable;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private boolean isPaused;

    public synchronized static ConnectionTools get() {
        if (_instance == null) {
            _instance = new ConnectionTools();
        }
        return _instance;
    }

    private ConnectionTools() {
        connectRunnable = new ConnectRunnable(this);
        disconnectRunnable = new DisconnectRunnable(this);
        requestRunnable = new DataRequestRunnable(this);
    }

    public void connect() {
        executor.execute(connectRunnable);
    }

    public void disconnect() {
        executor.execute(disconnectRunnable);
    }

    /**
     * Immediately requests data.
     */
    public void requestStatusData() {
        if (isPaused) {
            return;
        }
        executor.execute(requestRunnable);
    }

    /**
     * Requests data, delayed by 2 seconds.
     */
    public void requestStatusDataDelayed() {
        if (isPaused) {
            return;
        }
        executor.schedule(requestRunnable, 2, TimeUnit.SECONDS);
    }

    /**
     * Any request calls will be ignored until pause is disabled.
     */
    public synchronized void pause(boolean enable) {
        isPaused = enable;
    }

    /**
     * Whether request calls are currently ignored.
     */
    @Override
    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    @Override
    public DataInputStream getInputStream() {
        return inputStream;
    }

    @Override
    public DataOutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public synchronized void setSocket(Socket socket, InputStream in, OutputStream out) {
        this.socket = socket;
        this.inputStream = new DataInputStream(in);
        this.outputStream = new DataOutputStream(out);
    }
}
