package com.uwetrottmann.wpdisplay.util;

import com.uwetrottmann.wpdisplay.model.StatusData;
import de.greenrobot.event.EventBus;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

    /**
     * Maximum length of data read. Sent data is 183 bytes long, but we don't care about the rest,
     * yet.
     */
    private final static int RESPONSE_LENGTH_BYTES_MAX = 80;

    private final ConnectRunnable.ConnectListener listener;

    public DataRequestRunnable(ConnectRunnable.ConnectListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.listener = listener;
    }

    @Override
    public void run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

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

            EventBus.getDefault().post(new DataEvent(new StatusData(data)));
        } catch (IOException e) {
            Timber.e(e, "run: failed to request data");
        }
    }
}
