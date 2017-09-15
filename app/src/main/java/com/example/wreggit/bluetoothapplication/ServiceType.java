package com.example.wreggit.bluetoothapplication;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.List;

public class ServiceType {
    private BluetoothGattService mService;
    private List<BluetoothGattCharacteristic> mCharacteristics;

    ServiceType(BluetoothGattService service) {
        mService = service;
        mCharacteristics= new ArrayList<BluetoothGattCharacteristic>();
    }

    public BluetoothGattService getService() {return mService;}
    public List<BluetoothGattCharacteristic> getCharacteristics () {return mCharacteristics;}
}