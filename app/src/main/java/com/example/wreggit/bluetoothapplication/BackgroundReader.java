package com.example.wreggit.bluetoothapplication;

import android.os.AsyncTask;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

public class BackgroundReader extends AsyncTask<Void, Void, Void> {

    private ApplicationBluetoothManager mManager;
    public Queue<String> readingQueue = new LinkedList<>();

    public BackgroundReader(ApplicationBluetoothManager manager) {
        mManager = manager;
    }

    protected Void doInBackground(Void... params) {
        while(true) {
            if(isCancelled()) {
                break;
            }
            if(mManager.isConnected()) {
                readingQueue.add(ApplicationBluetoothManager.LAT_UUID);
                readingQueue.add(ApplicationBluetoothManager.LNG_UUID);
                mManager.read(ApplicationBluetoothManager.SERVICE_UUID, readingQueue.remove());
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Log.e("Background Reader", e.getMessage());
            }
        }

        return null;
    }
}
