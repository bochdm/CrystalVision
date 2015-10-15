package com.crystalvision;

import android.bluetooth.BluetoothSocket;

/**
 * Created by kseniaselezneva on 15/10/15.
 */
public interface CommunicatorService {
    Communicator createCommunicatorThread(BluetoothSocket socket);
}
