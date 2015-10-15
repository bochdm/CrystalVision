package com.crystalvision;

/**
 * Created by kseniaselezneva on 15/10/15.
 */
public interface Communicator {
    void startCommunication();
    void write(String message);
    void stopCommunication();

}
