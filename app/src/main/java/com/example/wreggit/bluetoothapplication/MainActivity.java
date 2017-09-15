package com.example.wreggit.bluetoothapplication;

import android.app.Activity;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private MapView mMapView;
    private Circle mainMarker;
    private ApplicationBluetoothManager mManager;
    private int retryCount = 5;
    private Activity mActivity;
    private TextView mLat, mLng;
    private ImageButton mConnectButton;
    private String lat = null;
    private String lng = null;
    private FloatingActionButton mLocationButton;

    private BackgroundReader mBackgroundReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivity = this;

        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mLat = (TextView) findViewById(R.id.lat);
        mLng = (TextView) findViewById(R.id.lng);
        mConnectButton = (ImageButton) mActivity.findViewById(R.id.static_reload);
        mLocationButton = (FloatingActionButton) mActivity.findViewById(R.id.floatingActionButton);

        this.requestPermissions(new String[] {android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        mManager = new ApplicationBluetoothManager(((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter(), getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ApplicationBluetoothManager.DEVICE_FOUND);
        intentFilter.addAction(ApplicationBluetoothManager.DEVICE_NOT_FOUND);
        intentFilter.addAction(ApplicationBluetoothManager.DEVICE_CONNECTED);
        intentFilter.addAction(ApplicationBluetoothManager.DEVICE_DISCONNECTED);
        intentFilter.addAction(ApplicationBluetoothManager.SERVICES_DISCOVERED);
        intentFilter.addAction(ApplicationBluetoothManager.READ_SUCCESS);
        LocalBroadcastManager.getInstance(this).registerReceiver(br, intentFilter);

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                retryCount = 5;
                StatusRunnable runnable = new StatusRunnable(mActivity, true, "Scanning for Devices...", true);
                runnable.run();
                mManager.cleanReset();
                mManager.startScan();
            }
        });

        mLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), 11));
            }
        });

        if(mManager.hasAddress()) {
            mManager.connect();
            StatusRunnable runnable = new StatusRunnable(this, true, "Found Device, Connecting...", true);
            runnable.run();
        } else {
            mManager.cleanReset();
            mManager.startScan();
            StatusRunnable runnable = new StatusRunnable(this, true, "Scanning for Devices...", true);
            runnable.run();
        }

        mMapView.getMapAsync(this);
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(mainMarker == null) {
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(new LatLng(0, 0));
            circleOptions.fillColor(Color.BLUE);
            circleOptions.strokeColor(Color.WHITE);
            circleOptions.radius(400);
            circleOptions.clickable(false);
            mainMarker = mMap.addCircle(circleOptions);
        }

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                CameraPosition cameraPosition = mMap.getCameraPosition();
                //Log.i("Zooming", "" + cameraPosition.zoom);
                mainMarker.setRadius(400);
                //mainMarker.setRadius((1 / Math.exp(cameraPosition.zoom)) * 25000000);
            }
        });


        if(mainMarker != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mainMarker.getCenter().latitude, mainMarker.getCenter().longitude), 11));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 11));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {}

    @Override
    protected void onPause() {
        super.onPause();
        mManager.disconnect();
        if(mBackgroundReader != null) {
            mBackgroundReader.cancel(true);
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(br);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mManager.cleanReset();
    }

    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ApplicationBluetoothManager.DEVICE_FOUND)) {
                Log.i("Received Device Address", intent.getStringExtra(ApplicationBluetoothManager.EXTRA_ADDRESS));
                mManager.stopScan();
                mActivity.runOnUiThread(new StatusRunnable(mActivity, true, "Found Device, Connecting...", null));
                mManager.connect();
            } else if(intent.getAction().equals(ApplicationBluetoothManager.DEVICE_NOT_FOUND)) {
                Log.i("Failed", "No Device Found");
                mActivity.runOnUiThread(new StatusRunnable(mActivity, true, "Device Not Found", false));
                mManager.cleanReset();
                retryCount = 5;
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mConnectButton.setVisibility(View.VISIBLE);
                    }
                });
            } else if (intent.getAction().equals(ApplicationBluetoothManager.DEVICE_CONNECTED)) {
                Log.i("Connected to Device", intent.getStringExtra(ApplicationBluetoothManager.EXTRA_ADDRESS));
                mManager.discoverServices();
                mActivity.runOnUiThread(new StatusRunnable(mActivity, true, "Device Connected, Discovering Services...", null));
            } else if (intent.getAction().equals(ApplicationBluetoothManager.DEVICE_DISCONNECTED)) {
                if(retryCount > 0 && intent.getStringExtra(ApplicationBluetoothManager.EXTRA_FULL_RESET) != null) {
                    mManager.cleanReset();
                    mManager.startScan();
                    retryCount--;
                    mActivity.runOnUiThread(new StatusRunnable(mActivity, true, "Failed to Connect, Retrying...", null));
                } else {
                    Log.i("Disconnected Device", "Stopping Scan to Save Battery");
                    mManager.stopScan();
                    mActivity.runOnUiThread(new StatusRunnable(mActivity, true, "Failed to Connect or Disconnected", false));
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                            public void run() {
                           mConnectButton.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } else if (intent.getAction().equals(ApplicationBluetoothManager.SERVICES_DISCOVERED)) {
                Log.i("Discovered All Services", intent.getStringExtra(ApplicationBluetoothManager.EXTRA_ADDRESS));

                boolean found = false;
                for(ServiceType service : mManager.getServices()) {
                    if(service.getService().getUuid().toString().equalsIgnoreCase(ApplicationBluetoothManager.SERVICE_UUID)) {
                        found = service.getCharacteristics().size() == 3;
                    }
                }

                if(found) {
                    mActivity.runOnUiThread(new StatusRunnable(mActivity, true, "Connected", false));
                    retryCount = 5;
                    mBackgroundReader = new BackgroundReader(mManager);
                    mBackgroundReader.execute();
                } else {
                    mManager.stopScan();
                    mManager.cleanReset();
                    mManager.startScan();
                    mActivity.runOnUiThread(new StatusRunnable(mActivity, true, "Not All Services Found", true));
                }

            } else if (intent.getAction().equals(ApplicationBluetoothManager.READ_SUCCESS)) {
                Log.i("READ SUCCESS", intent.getStringExtra(ApplicationBluetoothManager.EXTRA_CHARACTERISTIC));
                Log.i("READ SUCCESS", intent.getStringExtra(ApplicationBluetoothManager.EXTRA_VALUE));

                Double data = Double.parseDouble(intent.getStringExtra(ApplicationBluetoothManager.EXTRA_VALUE));
                data = data / 1000000;

                if(intent.getStringExtra(ApplicationBluetoothManager.EXTRA_CHARACTERISTIC).equalsIgnoreCase(ApplicationBluetoothManager.LAT_UUID)) {
                    lat = data.toString();
                } else if (intent.getStringExtra(ApplicationBluetoothManager.EXTRA_CHARACTERISTIC).equalsIgnoreCase(ApplicationBluetoothManager.LNG_UUID)) {
                    lng = data.toString();
                }

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLat.setText("Latitude: " + lat);
                        mLng.setText("Longitude: " + lng);
                        double doubleLat = 0.0, doubleLng = 0.0;
                        try {
                            doubleLat = Double.parseDouble(lat);
                            doubleLng = Double.parseDouble(lng);
                        } catch (NullPointerException e) { }
                        mainMarker.setCenter(new LatLng(doubleLat, doubleLng));
                    }
                });

                if(mBackgroundReader.readingQueue.size() > 0) {
                    mManager.read(ApplicationBluetoothManager.SERVICE_UUID, mBackgroundReader.readingQueue.remove());
                }
            }
        }
    };
}
