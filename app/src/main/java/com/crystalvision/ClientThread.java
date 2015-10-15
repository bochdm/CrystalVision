package com.crystalvision;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by kseniaselezneva on 15/10/15.
 */
public class ClientThread {
    private volatile Communicator communicator;

    private final BluetoothSocket socket;
    private BluetoothAdapter bluetoothAdapter;
    private final CommunicatorService communicatorService;

    public ClientThread(BluetoothDevice device, CommunicatorService communicatorService) {

        this.communicatorService = communicatorService;

        BluetoothSocket tmp = null;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(MainActivity.UUID));
        } catch (IOException e) {
            Log.d("ClientThread", e.getLocalizedMessage());
        }
        socket = tmp;
    }

    public synchronized Communicator getCommunicator() {
        return communicator;
    }

    public void run() {
        bluetoothAdapter.cancelDiscovery();
        try {
            Log.d("ClientThread", "About to connect");
            socket.connect();
            Log.d("ClientThread", "Connected");
            synchronized (this) {
                communicator = communicatorService.createCommunicatorThread(socket);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("ClientThread", "Start");
                    communicator.startCommunication();
                }
            }).start();
        } catch (IOException connectException) {
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.d("ClientThread", closeException.getLocalizedMessage());
            }
        }
    }

    public void cancel() {
        if (communicator != null) communicator.stopCommunication();
    }
}
