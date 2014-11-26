package com.uwetrottmann.wpdisplay.util;

import com.uwetrottmann.wpdisplay.model.StatusData;
import de.greenrobot.event.EventBus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import timber.log.Timber;

/**
 * Requests data from the heat pump controller, waits for a response and returns it through an
 * event.
 */
public class DataRequestRunnable implements Runnable {

    public static class DataEvent {
        public StatusData data;

        public DataEvent(StatusData data) {
            this.data = data;
        }
    }

    private final ConnectionListener listener;

    public DataRequestRunnable(ConnectionListener listener) {
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
        if (socket == null || !socket.isConnected()) {
            Timber.e("run: failed, no connection");
            return;
        }

        Timber.d("run: requesting status data");

        DataInputStream in = listener.getInputStream();
        DataOutputStream out = listener.getOutputStream();

        try {
            // skip any remaining data
            while (in.available() > 0) {
                in.readByte();
            }

            // send request
            out.writeInt(ControllerConstants.COMMAND_REQUEST_STATUS);
            out.writeInt(0);
            out.flush();

            // wait for and process data
            // heat pump controller sends 32bit BE integers
            // first integer should be sent command code
            int responseCode = in.readInt();
            if (responseCode != ControllerConstants.COMMAND_REQUEST_STATUS) {
                // fail
                Timber.e("run: response code expected " + ControllerConstants.COMMAND_REQUEST_STATUS
                        + " but was " + responseCode);
                return;
            }

            // ignored value
            in.readInt();
            // length (from server, so untrusted!)
            // cap maximum number of bytes read
            int length = Math.min(in.readInt(), StatusData.LENGTH_BYTES);

            // create array with max size
            int[] data = new int[StatusData.LENGTH_BYTES];

            // try reading sent data
            for (int i = 0; i < length; i++) {
                data[i] = in.readInt();
            }

            // don't update data if we have been paused
            if (listener.isPaused()) {
                Timber.d("run: not posting data, paused");
                return;
            }
            EventBus.getDefault().postSticky(new DataEvent(new StatusData(data)));
        } catch (IOException e) {
            Timber.e(e, "run: failed to request data");
        }
    }
}
