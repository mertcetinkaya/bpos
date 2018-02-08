package com.example.sony.bpos;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";


    private BluetoothAdapter mBluetoothAdapter;
    boolean bluetooth;

    private SensorManager mSensorManager;
    private boolean mScanning;
    private Handler mHandler;
    public EditText myEdit;
    List<String> list_device_address = new ArrayList<String>();
    List<Integer> list_rssi = new ArrayList<Integer>();
    List<String> list_device_address_first = new ArrayList<String>();
    List<String> list_device_address_second = new ArrayList<String>();
    List<String> list_device_address_far = new ArrayList<String>();

    private static final long SCAN_PERIOD = 7000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myEdit=(EditText)findViewById(R.id.editText);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


        AssetManager mngr = getAssets();

        if(getBaseContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED ){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        mHandler = new Handler();
        scanLeDevice(true);



    }

    private void clearList(List list){
        list.clear();
    }
    int count;
    int x=0;
    String closest_address;
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            myEdit.setText("");
            clearList(list_device_address);
            clearList(list_rssi);
            clearList(list_device_address_first);
            clearList(list_device_address_second);
            clearList(list_device_address_far);
            count=0;
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    myEdit.append("\n   Total beacon count: " + count);
                    //List<Object> objectList = Arrays.asList(list_rssi.toArray());
                    //Collections.sort(list_rssi, Collections.reverseOrder());
                    //x=list_rssi.get(0); //get first element
                    for (int i=0;i<count;i+=1){
                        if (list_rssi.get(i)>-83)
                            list_device_address_first.add(list_device_address.get(i));
                        else if (-83 >= list_rssi.get(i) && list_rssi.get(i)>-90)
                            list_device_address_second.add(list_device_address.get(i));
                        else
                            list_device_address_far.add(list_device_address.get(i));
                    }
                    closest_address =list_device_address.get(list_rssi.indexOf(Collections.max(list_rssi)));
                    myEdit.append("\n"+list_device_address+"\n");
                    myEdit.append("\n   Address of the closest beacon: \n");
                    myEdit.append(closest_address);
                    myEdit.append("\n   Adresses of the beacons in closeness from first degree");
                    myEdit.append(Arrays.toString(list_device_address_first.toArray()));
                    myEdit.append("\n   Adresses of the beacons in closeness from second degree");
                    myEdit.append(Arrays.toString(list_device_address_second.toArray()));
                    myEdit.append("\n   Adresses of the far beacons");
                    myEdit.append(Arrays.toString(list_device_address_far.toArray()));
                    //myEdit.append("\n   sorted: \n");
                    //myEdit.append(Arrays.toString(list_rssi.toArray()));
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(true);
                }
            }, SCAN_PERIOD+15000);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device,final int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ParcelUuid[] asdsa =  device.getUuids();
                            if(asdsa!=null) {
                                for (ParcelUuid uuid : asdsa) {
                                    Log.e("asdasda", "UUID: " + uuid.getUuid().toString());
                                }
                            }
                           String name = device.getName();
                           if (name != null) {
                               if (list_device_address.contains(device.getAddress()) == false && name.contains("EST")) {
                                   list_device_address.add(device.getAddress());
                                   list_rssi.add(rssi);
                                   count+=1;
                                   myEdit.append(device.getName() + " " + rssi + ", count: " + count + "\n");
                               }
                               //if (name.contains("EST")) {
                               //   myEdit.append(device.getName() + " " + rssi + "\n");
                               //}
                           }
                           //Log.e("asdasda",device.getName()+" "+" "+rssi+" "+device.getAddress());
                        }
                    });
                }
            };




}
