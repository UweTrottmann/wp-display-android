package com.uwetrottmann.wpdisplay.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Interfaces for {@link com.uwetrottmann.wpdisplay.util.ConnectionTools} runnables.
 */
public interface ConnectionListener {
    Socket getSocket();

    DataInputStream getInputStream();

    DataOutputStream getOutputStream();

    void setSocket(Socket socket, InputStream in, OutputStream out);

    boolean isPaused();
}
