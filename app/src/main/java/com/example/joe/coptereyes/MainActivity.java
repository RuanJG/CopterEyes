package com.example.joe.coptereyes;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private EditText consloe;
    private copterUartControler mUarts;
    private String bleName;
    private String bleAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button blebtn = (Button) findViewById(R.id.bleBtnId);
        Button tcpbtn = (Button) findViewById(R.id.tcpBtnId);
        consloe = (EditText) findViewById(R.id.consoleId);
        mUarts = new copterUartControler();
        mUarts.setupBleAndTcp(this.getApplicationContext(),mHandler,null,6666);
        if( blebtn!=null) blebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( mUarts.isBleDisconnected()){
                    findBleDevice(null,null);
                }else{
                    mUarts.doBleDisconnect();
                }

            }
        });
        if( tcpbtn!=null) tcpbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mUarts.isTcpServerDisconnected())
                    mUarts.doTcpServerConnect();
                else
                    mUarts.doTcpServerDisconnect();
            }
        });

        Button cleanBtn = (Button) findViewById(R.id.cleanBtnId);
        if(cleanBtn!=null)
            cleanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(consloe != null)
                        consloe.setText("");
                }
            });
    }

    private void findBleDevice(String jostickName, String jostickBtName)
    {//use this function when two jostick has their names
        Intent i;
        i = new Intent(this,BluetoothDevicesActivity.class);
        //i.putExtra(CameraJostickName, CameraJostickBtName);
        //i.putExtra(jostickName, jostickBtName);
        startActivityForResult(i, BluetoothDevicesActivity.REQUEST_ENABLE_BT);
    }

    private void doHandleMessage(Bundle data)
    {
        String id = data.getString("id");
        String name = data.getString("name");

        if( id.equals("onComError")){
            consloe.append(">>"+name+": "+id);
            //consloe.append(name+": "+id);
        }else if( id.equals("onStartingConnection") ) {
            consloe.append(">>"+name+": "+id);
            //consloe.append(name + " onStartingConnection");
        }else if( id.equals("onConnect")) {
            consloe.append(">>"+name+": "+id);
            //consloe.append(name);

        } else if (id.equals("onDisconnect")){
            consloe.append(">>"+name+": "+id);
            //onBleDisconnected(name);

        }else if (id.equals("onReceivePacket")){

        }else{
            ;//no fix msg
        }
    }

    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            /*
            if( mRcOutput == null) {
                super.handleMessage(msg);
                return;
            }*/
            switch (msg.what) {
                case 111:
                    doHandleMessage(msg.getData());
                default:
                    Log.e("RUAN", "unknow msg frome rcoutput");
                    break;
            }
            //super.handleMessage(msg);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothDevicesActivity.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_CANCELED) {
                    break;
                }
                String address;
                address = data.getStringExtra("address");
                mUarts.setAddress(address);
                mUarts.doBleConnect();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
