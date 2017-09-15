package com.example.wreggit.bluetoothapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class ApplicationBluetoothManager {


    public static String SERVICE_UUID = "00001821-0000-1000-8000-00805f9b34fb";
    public static String LNG_UUID = "00002AAF-0000-1000-8000-00805f9b34fb";
    public static String LAT_UUID = "00002AAE-0000-1000-8000-00805f9b34fb";
    private BluetoothAdapter mAdapter;
    private Context mContext;
    private BluetoothGatt mGatt;
    private String mAddress;
    private List<ServiceType> mServices = new ArrayList <>();
    private BluetoothDevice mDevice;
    private int mScanSeconds = 0;
    private boolean mConnected = false;

    public static final String DEVICE_FOUND = "com.bluetoothapp.DEVICE_FOUND";
    public static final String DEVICE_NOT_FOUND = "com.bluetoothapp.DEVICE_NOT_FOUND";
    public static final String DEVICE_CONNECTED ="com.bluetoothapp.DEVICE_CONNECTED";
    public static final String DEVICE_DISCONNECTED ="com.bluetoothapp.DEVICE_DISCONNECTED";
    public static final String SERVICES_DISCOVERED ="com.bluetoothapp.SERVICES_DISCOVERED";
    public static final String READ_SUCCESS = "com.bluetoothapp.READ_SUCCESS";
    public static final String EXTRA_FULL_RESET = "fullreset";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_CHARACTERISTIC  = "characteristic";
    public static final String EXTRA_VALUE = "value";
    public static final String EXTRA_VALUE_BYTE_ARRAY = "valueByteArray";

    public ApplicationBluetoothManager(BluetoothAdapter bluetoothAdapter, Context context) {
        mAdapter = bluetoothAdapter;
        mContext = context;
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if(mScanSeconds > 30) {
                mScanSeconds = 0;
                Intent failed = new Intent(DEVICE_NOT_FOUND);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(failed);
            }
            if(mAddress == null && !results.isEmpty()) {
                Log.i("Scan Callback", "Got Results " + results.get(0).getDevice().getAddress());
                mScanSeconds = 0;
                mAddress = results.get(0).getDevice().getAddress();
                Intent updateLocation = new Intent(DEVICE_FOUND);
                updateLocation.putExtra(EXTRA_ADDRESS, mAddress);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(updateLocation);
            } else {
                mScanSeconds++;
            }
        }
    };

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);



            if (status == 133 || status == 257) {
                if(Constant.DEBUG)
                    Log.i("JMG", "Unrecoverable error 133 or 257. DEVICE_DISCONNECTED intent broadcast with full reset");
                Intent intent = new Intent(DEVICE_DISCONNECTED);
                intent.putExtra(EXTRA_FULL_RESET, EXTRA_FULL_RESET);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS){ // Connected
                mGatt=gatt;
                if(Constant.DEBUG)
                    Log.i("JMG", "New connected Device. DEVICE_CONNECTED intent broadcast");

                mConnected = true;
                Intent intent = new Intent(DEVICE_CONNECTED);
                intent.putExtra(EXTRA_ADDRESS, gatt.getDevice().getAddress());
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                return;
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED && status == BluetoothGatt.GATT_SUCCESS){ // Connected
                if(Constant.DEBUG)
                    Log.i("JMG", "Disconnected Device. DEVICE_DISCONNECTED intent broadcast");
                Intent intent = new Intent(DEVICE_DISCONNECTED);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                return;
            }

            if(newState == BluetoothGatt.STATE_DISCONNECTED) {
                if(Constant.DEBUG)
                    Log.i("JMG", "Disconnected Device. DEVICE_DISCONNECTED intent broadcast");
                Intent intent = new Intent(DEVICE_DISCONNECTED);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                return;
            }

            if(Constant.DEBUG)
                Log.i("JMG", "Unknown values received onConnectionStateChange. Status: " + status + " - New state: " + newState);
        }

        @Override
        public void  onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(Constant.DEBUG)
                Log.i("JMG", "onServicesDiscovered status: " + status);

            for(BluetoothGattService serviceInList: gatt.getServices()){
                String serviceUUID=serviceInList.getUuid().toString();
                ServiceType serviceType = new ServiceType(serviceInList);
                List <BluetoothGattCharacteristic> characteristics= serviceType.getCharacteristics();
                if(Constant.DEBUG)
                    Log.i("JMG", "New service: " + serviceUUID);
                for(BluetoothGattCharacteristic characteristicInList : serviceInList.getCharacteristics()){
                    if(Constant.DEBUG)
                        Log.i("JMG", "New characteristic: " + characteristicInList.getUuid().toString());
                    characteristics.add(characteristicInList);
                }
                mServices.add(serviceType);
            }
            Intent intent = new Intent(SERVICES_DISCOVERED);
            intent.putExtra(EXTRA_ADDRESS, gatt.getDevice().getAddress());
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic chrc, int status) {
            super.onCharacteristicRead(gatt, chrc, status);

            if(status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }

            Intent intent = new Intent(READ_SUCCESS);
            intent.putExtra(EXTRA_CHARACTERISTIC, chrc.getUuid().toString());
            intent.putExtra(EXTRA_VALUE, new String(chrc.getValue()));
            intent.putExtra(EXTRA_VALUE_BYTE_ARRAY, chrc.getValue());
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
    };

    public void cleanReset() {
        mAddress = null;
        stopScan();
        disconnect();
        close();
        if (mServices != null) { mServices.clear(); }
    }

    public void connect() {
        mDevice = mAdapter.getRemoteDevice(mAddress);
        mServices.clear();
        if(mGatt != null){
            mGatt.connect();
        } else {
            mDevice.connectGatt(mContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        }
    }

    public void discoverServices() {
        if (Constant.DEBUG)
            Log.i("JMG", "Scanning services and characteristics");
        mGatt.discoverServices();
    }

    public void startScan() {
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setReportDelay(1000)
                .build();

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_UUID))).build();

        mAdapter.getBluetoothLeScanner().startScan(Arrays.asList(scanFilter), settings, mScanCallback);
    }

    public void stopScan() {
        mAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
    }

    public BluetoothGattCharacteristic findCharacteristic(String serviceUUID, String characteristicUUID){

        for (ServiceType serviceInList : mServices) {
            if (serviceInList.getService().getUuid().toString().equalsIgnoreCase(serviceUUID) ){
                for (BluetoothGattCharacteristic characteristicInList : serviceInList.getCharacteristics()) {
                    if (characteristicInList.getUuid().toString().equalsIgnoreCase(characteristicUUID) ){
                        return characteristicInList;
                    }
                }
            }
        }
        if(Constant.DEBUG)
            Log.i("JMG", "Characteristic not found. Service: " + serviceUUID + " Characteristic: " + characteristicUUID);
        return null;
    }

    public void read(String serviceUUID, String characteristicUUID){
        BluetoothGattCharacteristic characteristic = findCharacteristic(serviceUUID, characteristicUUID);
        if(characteristic!=null){
            mGatt.readCharacteristic(characteristic);
        } else {
            if(Constant.DEBUG) Log.i("JMG","Read Characteristic not found in device");
        }
    }

    public List<ServiceType> getServices() {
        return mServices;
    }

    public boolean isConnected(){ return mGatt != null; }

    public boolean hasAddress() { return mAddress != null; }

    public boolean disconnect() {
        if (mConnected && mGatt != null) {
            mGatt.disconnect();
            return true;
        }
        return false;
    }

    public void close() {
        if(mGatt != null) {
            mGatt.close();
            mGatt = null;
        }

        mConnected = false;
    }
}
