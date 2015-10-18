package com.crystalvision;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.SpeechRecognizer;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class BluetoothActivity extends ActionBarActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "CrystalVision";
    private Button button;
    private Button openMirror;
    private Button closeMirror;
    private TextView angleText;
    private ToggleButton toggle_discovery;
    private ListView listView;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ArrayAdapter<String> mArrayAdapter;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> pairedDevicesList;
    private ArrayList<String> unpairedDevicesList;
    private ArrayList<String> combinedDevicesList;
    private Set<BluetoothDevice> pairedDevices;
    private Set<String> unpairedDevices;
    private BroadcastReceiver mReceiver;
    private String selectedFromList;
    private String selectedFromListName;
    private String selectedFromListAddress;
    private BluetoothDevice crystalDevice;

    private SeekBar seekSetAngle;

       private static final String ADDRESS_CRYSTAL_VISION = "98:d3:31:fb:00:50";
   // private static final String ADDRESS_CRYSTAL_VISION = "8C:2D:AA:37:37:F0";

    private int mBindFlag;
    private static Messenger mServiceMessenger;

    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    final int STATE_CONNECTED = 2;
    final int STATE_CONNECTING = 1;
    final int STATE_DISCONNECTED = 0;
    private final UUID MY_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;
    public byte[] completeData;

    Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch(msg.what){
                case SUCCESS_CONNECT:
                    // Do Something;
                    ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
                    Toast.makeText(getApplicationContext(),"CONNECTED",Toast.LENGTH_LONG).show();
                    //String s = "This string proves a socket connection has been established!!";
                    String s = "test";
                    connectedThread.write(s.getBytes());
                    connectedThread.start();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[])msg.obj;
                    String string = new String(readBuf);
                    if (string.contains("!")){
                        //Do nothing!!
                    }else{
                        Toast.makeText(getApplicationContext(),string,Toast.LENGTH_SHORT).show();
                    }

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        button = (Button) findViewById(R.id.findCrystal);
        openMirror = (Button) findViewById(R.id.openMirror);
        closeMirror = (Button) findViewById(R.id.closeMirror);
     //   toggle_discovery =  (ToggleButton) findViewById(R.id.deviceDiscoverable);
        pairedDevicesList = new ArrayList<String>();
        unpairedDevicesList = new ArrayList<String>();
        unpairedDevices = new HashSet<String>();
        listView = (ListView)findViewById(R.id.listView);

        seekSetAngle = (SeekBar) findViewById(R.id.setAngle);
        angleText = (TextView) findViewById(R.id.angleText);

        // Sets up Bluetooth
        enableBT();

        button.setOnClickListener(this);
        openMirror.setOnClickListener(this);
        closeMirror.setOnClickListener(this);

        seekSetAngle.setOnSeekBarChangeListener(this);

        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // When clicked, show a toast with the TextView text
                selectedFromList = (String) (listView.getItemAtPosition(position));
		    		/*Debugging
		    		Toast.makeText(getApplicationContext(), selectedFromList,Toast.LENGTH_SHORT).show();*/
                String[] parts = selectedFromList.split(" ");
                selectedFromListName = parts[0];
                selectedFromListAddress = parts[1];
                BluetoothDevice selectedDevice = selectedDevice(selectedFromListAddress);
                mBluetoothAdapter.cancelDiscovery();
                ConnectThread ct = new ConnectThread(selectedDevice);
                ct.start();
                //ConnectThread ConnectThread = new ConnectThread(selectedDevice);
                //connectDevice();
		    		/* Debug Help
		    		Toast.makeText(getApplicationContext(), selectedFromListName,Toast.LENGTH_SHORT).show();
		    		Toast.makeText(getApplicationContext(), selectedFromListAddress,Toast.LENGTH_SHORT).show();
		    		Toast.makeText(getApplicationContext(),selectedDevice.getAddress(), Toast.LENGTH_SHORT).show();*/
            }
        });

        Intent service = new Intent(this, VoiceCommandService.class);//.putExtra(PARAM_PINTENT, pi);
        startService(service);

        mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 0 : Context.BIND_ABOVE_CLIENT;

    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("CrystalVision", "onServiceConnected");

            mServiceMessenger = new Messenger(service);
            Message msg = new Message();
            msg.what = VoiceCommandService.MSG_RECOGNIZER_START_LISTENING;

            try{
                mServiceMessenger.send(msg);
            }catch (RemoteException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("CrystalVision", "onServiceDisconnected");
            mServiceMessenger = null;
        }

    }; // mServiceConnection

    @Override
    protected void onStart() {
        super.onStart();

        bindService(new Intent(this, VoiceCommandService.class), mServiceConnection, mBindFlag);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mServiceMessenger != null) {
            unbindService(mServiceConnection);
            mServiceMessenger = null;
        }
    }

    public void displayCominedDevices(){
   //     displayPairedDevices();
        displayDetectedDevices();
        mArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,removeDuplicates(unpairedDevicesList,pairedDevicesList));
        listView.setAdapter(mArrayAdapter);

        connect();
    }

    OutputStream mmOutStream;

    private void connect(){
        BluetoothSocket mmSocket = null;
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            mmSocket = crystalDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) { }

        if (mmSocket != null) {
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                mmOutStream = mmSocket.getOutputStream();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Toast.makeText(getApplicationContext(), "Connecting to device failed!", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public BluetoothDevice selectedDevice(String deviceAddress){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device;
    //    device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        device = mBluetoothAdapter.getRemoteDevice("8C:2D:AA:37:37:F0");
        return device;
    }

    @SuppressLint("NewApi")
    public String checkState(BluetoothSocket mmSocket2){
        String state = "NOT_KNOWN";

        if (mmSocket2.isConnected() == true){
            state = "STATE_CONNECTED";
        }
        state = "STATE_DISCONNECTED";

        Toast.makeText(getApplicationContext(), state, Toast.LENGTH_SHORT).show();

        return state;
    }


    @SuppressWarnings("unchecked")
    public ArrayList<String> removeDuplicates(ArrayList<String> s1, ArrayList<String> s2){
		/*Debugging
		Toast.makeText(getApplication(), "unpairedList " + s1.toString(),Toast.LENGTH_LONG).show();
		Toast.makeText(getApplication(), "pairedList " + s2.toString(),Toast.LENGTH_LONG).show(); */
        combinedDevicesList =  new ArrayList<String>();
        combinedDevicesList.addAll(s1);
        combinedDevicesList.addAll(s2);
        @SuppressWarnings("unchecked")
        Set Unique_set = new HashSet(combinedDevicesList);
        combinedDevicesList = new ArrayList<String>(Unique_set);
		/*Debugging
		Toast.makeText(getApplication(),"Combined List" + combinedDevicesList.toString(),Toast.LENGTH_LONG).show(); */
        return combinedDevicesList;
    }

    public void enableBT(){
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Bluetooth is not suppourted on Device",Toast.LENGTH_SHORT).show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            int resultCode = Activity.RESULT_OK;
            if(resultCode < 1){
                Toast.makeText(getApplicationContext(), "Please Accept Enabling Bluetooth Request!", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Enabling Bluetooth FAILED!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void displayPairedDevices(){
        // If there are paired devices
        enableBT();
        if (pairedDevices.size() > 0) {
            //Toast.makeText(getApplicationContext(),"in loop",Toast.LENGTH_SHORT).show();
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                String s = " ";
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                pairedDevicesList.add(deviceName + s + deviceAddress +" \n");
            }
        }
    }

    public void displayDetectedDevices(){
        mBluetoothAdapter.startDiscovery();

        // Create a BroadcastReceiver for ACTION_FOUND
        mReceiver = new BroadcastReceiver() {
            @SuppressWarnings("static-access")
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
		        /* Debugging help
		        Toast.makeText(getApplicationContext(),action,Toast.LENGTH_SHORT).show();*/
                // When discovery finds a device
                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		        	/* Debugging help
		        	Toast.makeText(getApplicationContext(),device.getName(),Toast.LENGTH_SHORT).show();*/
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    String s = " ";
                    unpairedDevices.add(deviceName + s + deviceAddress +" \n");
                    //unpairedDevicesList.add(deviceName + s + deviceAddress +" (un-paired)\n");
                    unpairedDevicesList = new ArrayList<String>(unpairedDevices);
                }
            }
        };
		/*adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,unpairedDevicesList);
		listView.setAdapter(adapter);*/
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

    }

    public void makeDicoverable(int option){
        Intent discoverableIntent;
        if (option == 1){
            discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,120);
            startActivity(discoverableIntent);


            Toast.makeText(getApplicationContext(), "Open discovery for 2mins", Toast.LENGTH_SHORT).show();
        } else {
            discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
            startActivity(discoverableIntent);
            Toast.makeText(getApplicationContext(), "Open discovery is OFF!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void sendVoicedata(Bundle results){
        if (results != null){
            ArrayList<String> stringArrayList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.i(TAG, "stringArrayList -> " + stringArrayList.size());
            StringBuilder sb = new StringBuilder();
            for (String text : stringArrayList) {
                sb.append(text);
                Log.i(TAG, text);
            }
            String voiceText = sb.toString();

            if (voiceText.toLowerCase().contains("открыть")){
             //   sendData("OPEN");
            }
            Arrays.asList("Открыть", "open").contains(voiceText);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.findCrystal:
                Toast.makeText(getApplicationContext(), "Searching for devices, please wait... ",Toast.LENGTH_SHORT).show();
                // Checks for known paired devices
             //   pairedDevices = new HashSet<>();
                pairedDevices = mBluetoothAdapter.getBondedDevices();

                try {
               //     crystalDevice = mBluetoothAdapter.getRemoteDevice(ADDRESS_CRYSTAL_VISION);
                  //  pairedDevices.add(crystalDevice);
               //     pairedDevices.add(pairedDevices);
                    displayCominedDevices();
                }catch (Exception e){
                    Toast.makeText(this, "CrystalVision not found", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.openMirror:
                Log.i(TAG, "openMirror");
                sendData("OPEN");
                break;
            case R.id.closeMirror:
                Log.i(TAG, "openMirror");

                sendData("CLOSE");
                break;
        }
    }

    private void sendData(String data) {
        byte[] bytes = data.getBytes();

        if (bytes.length > 0 && mmOutStream != null) {
            try {
                mmOutStream.write(bytes);

            } catch (IOException e) {
                Log.e(TAG, "mmOutStream error");
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        angleText.setText(seekBar.getProgress() + "");
        sendData("v:" + seekBar.getProgress());
    }

    @SuppressLint("NewApi")
    public class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;

            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Toast.makeText(getApplicationContext(), "Connecting to device failed!", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }

    }
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer; // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    buffer = new byte[9800];
                    bytes = mmInStream.read(buffer,0,buffer.length);

                    // Send the obtained bytes to the UI activity
                //    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                    write("test".getBytes());

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);


            } catch (IOException e) { }
        }
    }

}
