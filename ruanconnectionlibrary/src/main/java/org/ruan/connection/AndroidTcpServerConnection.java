package org.ruan.connection;

import java.io.File;
import java.io.IOException;
import org.ruan.connection.AndroidMavLinkConnection;
import android.content.Context;

public class AndroidTcpServerConnection extends AndroidMavLinkConnection {

    private final TcpServerConnection mConnectionImpl;
    private final String serverIp;
    private final int serverPort;

    public AndroidTcpServerConnection(Context context, String tcpServerIp, int tcpServerPort) {
        super(context);
        this.serverIp = tcpServerIp;
        this.serverPort = tcpServerPort;

        mConnectionImpl = new TcpServerConnection() {
            @Override
            protected int loadServerPort() {
                return serverPort;
            }

            @Override
            protected String loadServerIP() {
                return serverIp;
            }

            @Override
            protected Logger initLogger() {
                return AndroidTcpServerConnection.this.initLogger();
            }

            @Override
            protected void onConnectionOpened(){
                AndroidTcpServerConnection.this.onConnectionOpened();
            }

            @Override
            protected void onConnectionFailed(String errMsg){
                AndroidTcpServerConnection.this.onConnectionFailed(errMsg);
            }
        };
    }

    @Override
    protected void closeConnection() throws IOException {
        mConnectionImpl.closeConnection();
    }

    @Override
    protected void loadPreferences() {
        mConnectionImpl.loadPreferences();
    }

    @Override
    protected void openConnection() throws IOException {
        mConnectionImpl.openConnection();
    }

    @Override
    protected int readDataBlock(byte[] buffer) throws IOException {
        return mConnectionImpl.readDataBlock(buffer);
    }

    @Override
    protected void sendBuffer(byte[] buffer) throws IOException {
        mConnectionImpl.sendBuffer(buffer);
    }

    @Override
    public int getConnectionType() {
        return mConnectionImpl.getConnectionType();
    }
}
