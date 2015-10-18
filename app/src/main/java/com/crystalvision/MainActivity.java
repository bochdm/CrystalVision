package com.crystalvision;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends ActionBarActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener,
        AdapterView.OnItemClickListener,
        RecognitionListener {

    public static final String MYUUID = "bcc8b300-736c-11e5-a837-0800200c9a66";

    private final static int REQUEST_ENABLE_BT = 1;
 //   private static final String ADDRESS_CRYSTAL_VISION = "98:d3:31:fb:00:50";
    private static final String ADDRESS_CRYSTAL_VISION = "8C:2D:AA:37:37:F0";
    private static final int REQUEST_OK = 1;
    private static final int SPEECH_REQUEST_CODE = 0;
    private static final int RECOGNIZE_SPEECH_CODE = 3;
    private static final int VOICE_SERVICE = 1;
    private static final String PARAM_PINTENT = "peindingIntent";

    private BluetoothAdapter adapter;
    private BroadcastReceiver discoverDevicesReceiver;
    private BroadcastReceiver discoveryFinishedReceiver;
    private TextView textView;
    private TextView textAngle;
    private SeekBar seekAngle;
    private ListView listDevices;
    private ProgressDialog progressDialog;
    private Button openMirror;
    private Button closeMirror;
    private ImageButton speech;
    private Intent speechRecIntent;

    private SpeechRecognizer speechRec = null;

    private ArrayAdapter<BluetoothDevice> listAdapter;

    private int mBindFlag;
    private static Messenger mServiceMessenger;

    private final List<BluetoothDevice> discoveredDevices = new ArrayList<BluetoothDevice>();

    private ClientThread clientThread;

    private final CommunicatorService communicatorService = new CommunicatorService() {
        @Override
        public Communicator createCommunicatorThread(BluetoothSocket socket) {
            return new CommunicatorImpl(socket, new CommunicatorImpl.CommunicationListener() {
                @Override
                public void onMessage(final String message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(textView.getText().toString() + "\n" + message);
                        }
                    });
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = BluetoothAdapter.getDefaultAdapter();

        Button btnCheckBT = (Button) findViewById(R.id.check_bluetooth);
        textView = (TextView) findViewById(R.id.textView);
        textAngle = (TextView) findViewById(R.id.angle);
        seekAngle = (SeekBar) findViewById(R.id.setAngle);
        listDevices = (ListView) findViewById(R.id.listView);
        openMirror = (Button) findViewById(R.id.openMirror);
        closeMirror = (Button) findViewById(R.id.closeMirror);
        speech = (ImageButton) findViewById(R.id.speech);

        btnCheckBT.setOnClickListener(this);

        seekAngle.setOnSeekBarChangeListener(this);

        listAdapter = new ArrayAdapter<BluetoothDevice>(getBaseContext(), android.R.layout.simple_list_item_2,android.R.id.text1, discoveredDevices) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                final BluetoothDevice device = getItem(position);
                ((TextView) view.findViewById(android.R.id.text1)).setText(device.getName());
                ((TextView) view.findViewById(android.R.id.text2)).setText(device.getAddress());
                return view;
            }
        };

        listDevices.setAdapter(listAdapter);
        listDevices.setOnItemClickListener(this);

        openMirror.setOnClickListener(this);
        closeMirror.setOnClickListener(this);
        speech.setOnClickListener(this);

   //     PendingIntent pi = createPendingResult(VOICE_SERVICE, null, 0);

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

    @Override
    protected void onResume() {
        super.onResume();

/*
        speechRec = SpeechRecognizer.createSpeechRecognizer(this);
        speechRec.setRecognitionListener(this);

        speechRecIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        speechRecIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());

        speechRec.startListening(speechRecIntent);
*/

    }

    private void checkBT(){
        if (adapter != null){
            if (adapter.isEnabled()){
                String address = adapter.getAddress();
                String name = adapter.getName();
                int state = adapter.getState();

                textView.setText(address + " " + name);

            //    Toast.makeText(this, state, Toast.LENGTH_SHORT).show();

            }else {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

/*        if (requestCode == REQUEST_OK && resultCode ==REQUEST_OK){
            ArrayList<String> thingsYouSaid = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            textView.setText(thingsYouSaid.get(0));
            sendData(thingsYouSaid.get(0));
        }*/

     /*   if (requestCode == VOICE_SERVICE){
            Log.i("CrystalVision", "Voice_service");
        }*/

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            textView.setText(spokenText);
            sendData(spokenText);

            // Do something with spokenText
        }

        if (requestCode == RECOGNIZE_SPEECH_CODE && resultCode == RESULT_OK) {
            if (data != null){
                ArrayList<String> stringArrayList = data.getStringArrayListExtra(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.i("CrystalVision", "stringArrayList -> " + stringArrayList.size());
                for (String text : stringArrayList) {
                    Log.i("CrystalVision", text);
                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.check_bluetooth:
                checkBT();
                break;
            case R.id.openMirror:
           //     sendMessage("OPEN");
                sendData("OPEN");

                break;
            case R.id.closeMirror:
             //   sendMessage("CLOSE");
                sendData("CLOSE");

                break;
            case R.id.speech:
                sendVoiceData();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        textAngle.setText(progress + "");
      //  Log.i("CrystalVision", "progress -> " + progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {


    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
      //  sendMessage(String.valueOf(seekBar.getProgress()));
        Log.i("CrystalVision", "progress -> " + seekBar.getProgress());
        sendData("v:" + String.valueOf(seekBar.getProgress()));
    }

    private void sendVoiceData(){
       /* Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
        try {
            startActivityForResult(i, REQUEST_OK);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing speech to text engine.", Toast.LENGTH_LONG).show();
        }*/
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
           // intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        }catch (ActivityNotFoundException anfe){
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,   Uri.parse("https://market.android.com/details?id=com.google.android.voicesearch"));
            startActivity(browserIntent);
        }
    }

    private void sendData(String message){
        if (crystalSocket == null){
            Toast.makeText(this, "CrystalVision not found. Please pess button 'Find Crystal'", Toast.LENGTH_SHORT).show();
        }

        byte[] bytes = message.getBytes();
        if (bytes.length == 0){
            return;
        }

        if (crystalStream != null) {
            try {
                crystalStream.write(bytes);
                Log.i("CrystalVision", "write to stream");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            Toast.makeText(this, "Подключите устройство", Toast.LENGTH_SHORT).show();
        }
    }

    private BluetoothSocket crystalSocket;
    private OutputStream crystalStream;

    public void findCrystal(View view) {
        BluetoothDevice crystalDevice = adapter.getRemoteDevice(ADDRESS_CRYSTAL_VISION);
        try{
            crystalSocket = crystalDevice.createRfcommSocketToServiceRecord(UUID.fromString(MYUUID));

        }catch (IOException ioe){
            Log.e("CrystalVision", "can't fid CrytalVision device");
        }

        adapter.cancelDiscovery();

        try{
            crystalSocket.connect();
            Log.i("CrystalVision", "crystalSocket is connected");
        }catch (IOException ioe){
            try {
                Log.i("CrystalVision", "crystalSocket is closed");

                crystalSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try{
            crystalStream = crystalSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        textView.setText(crystalDevice.getAddress() + " " + crystalDevice.getName());
    }


    public void discoverDevices(View view) {

        discoveredDevices.clear();
        listAdapter.notifyDataSetChanged();

        if (discoverDevicesReceiver == null) {
            discoverDevicesReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                        if (!discoveredDevices.contains(device)) {
                            discoveredDevices.add(device);
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                }
            };
        }

        if (discoveryFinishedReceiver == null) {
            discoveryFinishedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    listDevices.setEnabled(true);
                    if (progressDialog != null)
                        progressDialog.dismiss();
                    Toast.makeText(getBaseContext(), "Поиск закончен. Выберите устройство для отправки ообщения.", Toast.LENGTH_LONG).show();
                    unregisterReceiver(discoveryFinishedReceiver);
                }
            };
        }

        registerReceiver(discoverDevicesReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(discoveryFinishedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        listDevices.setEnabled(false);

        progressDialog = ProgressDialog.show(this, "Поиск устройств", "Подождите...");

        adapter.startDiscovery();
        BluetoothDevice remoteDevice = adapter.getRemoteDevice("8C:2D:AA:37:37:F0");
        //remoteDevice.
      //  remoteDevice.set
    }

    @Override
    public void onPause() {
        super.onPause();
        adapter.cancelDiscovery();

        if (discoverDevicesReceiver != null) {
            try {
                unregisterReceiver(discoverDevicesReceiver);
            } catch (Exception e) {
                Log.d("MainActivity", "Не удалось отключить ресивер " + discoverDevicesReceiver);
            }
        }

        if (clientThread != null) {
            clientThread.cancel();
        }

        if (crystalStream != null) {
            try {
                crystalStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (crystalSocket != null) {
            try {
                crystalSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (speechRec != null) {
            speechRec.destroy();
        }
        //   if (serverThread != null) serverThread.cancel();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (clientThread != null) {
            clientThread.cancel();
        }

        BluetoothDevice deviceSelected = discoveredDevices.get(position);

        clientThread = new ClientThread(deviceSelected, communicatorService);
        clientThread.start();

        Toast.makeText(this, "Вы подключились к устройству \"" + discoveredDevices.get(position).getName() + "\"", Toast.LENGTH_SHORT).show();


    }


    public static void sendMessage(int type){

        Message msg = new Message();
        msg.what = type;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        if (clientThread != null) {
            //Log.i("CrystalVision", message);
            //Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            new WriteTask().execute(message);
        } else {
            Toast.makeText(this, "Сначала выберите клиента", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        speechRec.startListening(speechRecIntent);
    }

    @Override
    public void onError(int error) {
        if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
            Log.d("CrystalVision", "client error");
        }else{
            speechRec.startListening(speechRecIntent);
        }
    }

    @Override
    public void onResults(Bundle results) {
     /*   if (results != null){
            ArrayList<String> stringArrayList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.i("CrystalVision", "stringArrayList -> " + stringArrayList.size());
            for (String text : stringArrayList) {
                Log.i("CrystalVision", text);
            }
        }*/
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }


    private class WriteTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... args) {
            try {
                Log.i("CrystalVision", args[0]);
                clientThread.getCommunicator().write(args[0]);
                Toast.makeText(getApplicationContext(), args[0], Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.d("MainActivity", e.getClass().getSimpleName() + " " + e.getLocalizedMessage());
            }
            return null;
        }
    }

    // 8C:2D:AA:37:37:F0
}
