package com.uwetrottmann.wpdisplay.util;

import android.content.Context;
import com.uwetrottmann.wpdisplay.settings.ConnectionSettings;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import timber.log.Timber;

public class ConnectionTools implements ConnectionListener {

    private static ConnectionTools _instance;

    public static class ConnectionEvent {
        public boolean isConnecting;
        public boolean isConnected;
        public String host;
        public int port;

        public ConnectionEvent(boolean isConnecting, boolean isConnected, String host, int port) {
            this.isConnecting = isConnecting;
            this.isConnected = isConnected;
            this.host = host;
            this.port = port;
        }
    }

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private final DisconnectRunnable disconnectRunnable;
    private final DataRequestRunnable requestRunnable;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private ScheduledFuture<?> requestSchedule;
    private boolean isPaused;

    public synchronized static ConnectionTools get() {
        if (_instance == null) {
            _instance = new ConnectionTools();
        }
        return _instance;
    }

    private ConnectionTools() {
        disconnectRunnable = new DisconnectRunnable(this);
        requestRunnable = new DataRequestRunnable(this);
    }

    /**
     * Try to establish a connection, async.
     */
    public synchronized void connect(Context context) {
        Timber.d("connect: scheduling");
        executor.execute(new ConnectRunnable(this, ConnectionSettings.getHost(context),
                ConnectionSettings.getPort(context)));
    }

    /**
     * Disconnect (if connected) and stop status data requests.
     */
    public synchronized void disconnect() {
        Timber.d("disconnect: scheduling");
        cancelStatusDataRequests();
        executor.execute(disconnectRunnable);
    }

    /**
     * If enabled, will request new status data immediately, then every 2 seconds.
     *
     * <p> <b>Note:</b> If already enabled/disabled or paused, will do nothing.
     */
    public synchronized void requestStatusData(boolean enable) {
        if (isPaused) {
            // do nothing, paused
            return;
        }

        if (enable) {
            scheduleStatusDataRequests();
        } else {
            cancelStatusDataRequests();
        }
    }

    /**
     * Stop requesting status data.
     */
    public synchronized void pause() {
        isPaused = true;
        cancelStatusDataRequests();
    }

    /**
     * Resume requesting status data.
     */
    public synchronized void resume() {
        isPaused = false;
        scheduleStatusDataRequests();
    }

    private void scheduleStatusDataRequests() {
        if (requestSchedule != null) {
            // already running
            return;
        }
        Timber.d("scheduleStatusDataRequests: scheduling");
        requestSchedule = executor.scheduleWithFixedDelay(requestRunnable, 0, 2,
                TimeUnit.SECONDS);
    }

    private void cancelStatusDataRequests() {
        if (requestSchedule != null) {
            requestSchedule.cancel(true);
            requestSchedule = null;
        }
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
