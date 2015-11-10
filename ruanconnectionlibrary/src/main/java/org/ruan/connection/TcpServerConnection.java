package org.ruan.connection;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by joe on 2015/11/9.
 */
public abstract class TcpServerConnection extends MavLinkConnection{
    private static final int CONNECTION_TIMEOUT = 20 * 1000; // 20 secs in ms

    private Socket socket;
    private BufferedOutputStream mavOut;
    private BufferedInputStream mavIn;

    private String serverIP;
    private int serverPort;
    private boolean clientConnected = false;

    @Override
    public final void openConnection() throws IOException {
        getTCPStream();
        onConnectionOpened();
    }

    @Override
    public final int readDataBlock(byte[] buffer) throws IOException {
        return mavIn.read(buffer);
    }

    @Override
    public final void sendBuffer(byte[] buffer) throws IOException {
        if (mavOut != null) {
            mavOut.write(buffer);
            mavOut.flush();
        }
    }

    @Override
    public final void loadPreferences() {
        serverIP = loadServerIP();
        serverPort = loadServerPort();
    }

    protected abstract int loadServerPort();

    protected abstract String loadServerIP();

    @Override
    public final void closeConnection() throws IOException {
        if (socket != null)
            socket.close();
        if( mServerSocket != null){
            mServerSocket.close();
            mServerSocket=null;
        }
    }

    private ServerSocket mServerSocket=null;

    private void getTCPStream() throws IOException {
        if( mServerSocket==null || mServerSocket.isClosed())
            mServerSocket = new ServerSocket(serverPort,1);
        socket = mServerSocket.accept();
        mavOut = new BufferedOutputStream((socket.getOutputStream()));
        mavIn = new BufferedInputStream(socket.getInputStream());
    }

    @Override
    public final int getConnectionType() {
        return MavLinkConnectionTypes.MAVLINK_CONNECTION_TCP;
    }
}
