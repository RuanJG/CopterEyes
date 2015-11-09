package com.example.joe.coptereyes;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.MAVLinks.MAVLinkPacket;
import com.MAVLinks.Parser;

import org.ruan.connection.BluetoothConnection;
import org.ruan.connection.MavLinkConnection;
import org.ruan.connection.MavLinkConnectionListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by joe on 2015/11/9.
 */
public class copterUartControler {
    private BluetoothConnection mBleConnect;
    private  Handler mHandler;
    private  String mName="copterBleConnection";
    private  Context mContext;
    private String mAddress;

    private int mTcpPort = 6666;
    private final int READ_BUFFER_SIZE = 2048;
    private ServerSocket mServerSocket;
    private Socket mSocket;
    private BufferedOutputStream mavOut;
    private BufferedInputStream mavIn;

    private final LinkedBlockingQueue<byte[]> mPacketsToBleSend = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<byte[]> mPacketsToTcpSend = new LinkedBlockingQueue<>();

    private String TAG= "copterUartControler";

    public void setupBleAndUart(Context c,Handler h,String address) {
        mHandler = h;
        mContext = c;
        mAddress = address;
    }

    public void setAddress(String adr) { mAddress = adr;}
    public String getAddress(){ return mAddress;}
    public void setName(String name) { mName = name; }
    public String getName(){ return mName;}

    public boolean isBleDisconnected()
    {
        return mBleConnect==null || mBleConnect.getConnectionStatus() == MavLinkConnection.MAVLINK_DISCONNECTED;
    }

    public void doBleConnect()
    {
        if( mAddress != null && isBleDisconnected()){
            //if( mBleConnect == null) {
            mBleConnect = new BluetoothConnection(mContext, mAddress);
            mBleConnect.addMavLinkConnectionListener(mName, mBlelistener);
            //}
            mBleConnect.connect();
        }
    }
    public void doBleDisconnect()
    {
        if( isBleDisconnected()) return;

        mBleConnect.removeMavLinkConnectionListener(mName);
        if (mBleConnect.getMavLinkConnectionListenersCount() == 0 && mBleConnect.getConnectionStatus() != MavLinkConnection.MAVLINK_DISCONNECTED) {
            //Timber.d("Disconnecting...");
            mBleConnect.disconnect();
        }
        //as i removelisten , onDisconnect will not recived
        Message message = new Message();
        message.what = 111;
        Bundle bundle = new Bundle();
        bundle.putLong("time", 0);
        bundle.putString("id", "onDisconnect");
        bundle.putString("name",mName);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    private MavLinkConnectionListener mBlelistener=new MavLinkConnectionListener() {
        @Override
        public void onStartingConnection() {
            Message message = new Message();
            message.what = 111;
            Bundle bundle = new Bundle();
            bundle.putString("name",mName);
            bundle.putString("id","onStartingConnection");
            message.setData(bundle);
            mHandler.sendMessage(message);
        }

        @Override
        public void onConnect(long connectionTime) {
            Message message = new Message();
            message.what = 111;
            Bundle bundle = new Bundle();
            bundle.putLong("time",connectionTime);
            bundle.putString("id", "onConnect");
            bundle.putString("name",mName);
            message.setData(bundle);
            mHandler.sendMessage(message);
        }

        @Override
        public void onReceivePacket(MAVLinkPacket packet) {
                Message message = new Message();
                Bundle bundle = new Bundle();
                message.what = 111;
                bundle.putString("id", "onReceivePacket");
                bundle.putString("name",mName);
                message.setData(bundle);
                mHandler.sendMessage(message);
        }

        @Override
        public void onDisconnect(long disconnectionTime) {
            Message message = new Message();
            message.what = 111;
            Bundle bundle = new Bundle();
            bundle.putLong("time",disconnectionTime);
            bundle.putString("id", "onDisconnect");
            bundle.putString("name",mName);
            message.setData(bundle);
            mHandler.sendMessage(message);
        }

        @Override
        public void onComError(String errMsg) {
            Message message = new Message();
            message.what = 111;
            Bundle bundle = new Bundle();
            bundle.putString("string",errMsg);
            bundle.putString("id","onComError");
            bundle.putString("name",mName);
            message.setData(bundle);
            mHandler.sendMessage(message);
        }
    };





    public final int readDataBlock(byte[] buffer) throws IOException {
        return mavIn.read(buffer);
    }
    public final void sendBuffer(byte[] buffer) throws IOException {
        if (mavOut != null) {
            mavOut.write(buffer);
            mavOut.flush();
        }
    }
    private void getTCPStream() throws IOException {
        mServerSocket = new ServerSocket(mTcpPort);
        mSocket= mServerSocket.accept();
        mavOut = new BufferedOutputStream((mSocket.getOutputStream()));
        mavIn = new BufferedInputStream(mSocket.getInputStream());
    }
    private final Runnable mSendingTask = new Runnable() {
        @Override
        public void run() {
            try {
                while (mConnectionStatus.get() == MAVLINK_CONNECTED) {
                    byte[] buffer = mPacketsToSend.take();

                    try {
                        sendBuffer(buffer);
                        queueToLog(buffer);
                    } catch (IOException e) {
                        reportComError(e.getMessage());
                        mLogger.logErr(TAG, e);
                    }
                }
            } catch (InterruptedException e) {
                mLogger.logVerbose(TAG, e.getMessage());
            } finally {
                disconnect();
            }
        }
    };
    private final Runnable mManagerTask = new Runnable() {

        @Override
        public void run() {
            Thread sendingThread = null;

            try {
                final long connectionTime = System.currentTimeMillis();
                //mConnectionTime.set(connectionTime);
                //reportConnect(connectionTime);

                // Launch the 'Sending' thread
                sendingThread = new Thread(mSendingTask, "CopterEye-Tcp-Sending Thread");
                sendingThread.start();

                final Parser parser = new Parser();
                parser.stats.mavlinkResetStats();

                final byte[] readBuffer = new byte[READ_BUFFER_SIZE];

                while (mConnectionStatus.get() == MAVLINK_CONNECTED) {
                    int bufferSize = readDataBlock(readBuffer);
                    //handleData(parser, bufferSize, readBuffer);
                }
            } catch (IOException e) {
                // Ignore errors while shutting down
                if (mConnectionStatus.get() != MAVLINK_DISCONNECTED) {
                    reportComError(e.getMessage());
                    mLogger.logErr(TAG, e);
                }
            } finally {
                if (sendingThread != null && sendingThread.isAlive()) {
                    sendingThread.interrupt();
                }

                if (loggingThread != null && loggingThread.isAlive()) {
                    loggingThread.interrupt();
                }

                disconnect();
                Log.e(TAG, "Exiting manager thread.");
            }
        }
    };


    private  void doTcpServerStart()
    {

    }



}
