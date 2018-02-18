package com.example.sony.bpos;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.EditText;

import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import android.os.Environment;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.Object;
import java.lang.Integer;



public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1234;

    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 123456;
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
    List<String> list_device_address_closest = new ArrayList<String>();

    ToggleButton logbut;
    EditText logname;
    boolean is_pressed=false;
    String tag;



    //List<String> list_device_address_to_save = new ArrayList<String>(Arrays.asList ( "C9:00:6A:7D:EF:B8" , "C5:EC:3D:11:FB:31" , "CB:7F:3D:BD:0D:26", "FD:15:89:12:5C:2E" ));
    List<String> list_to_write = new ArrayList<String>();



    private static final long SCAN_PERIOD = 2000;
    private static final long RESTING_PERIOD = 4000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myEdit=(EditText)findViewById(R.id.editText);
        logname=(EditText)findViewById(R.id.textView);
        logbut=(ToggleButton) findViewById(R.id.toggleButton);
        logbut.setChecked(false);
        logbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                is_pressed = logbut.isChecked();
                if(is_pressed) {
                    tag = logname.getText().toString();
                }
            }
        });

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

        if (getBaseContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
        }



    }


    private void writeFile(String data,String fileName) {

        File extStore = Environment.getExternalStorageDirectory();
        // ==> /storage/emulated/0/note.txt
        String path =  "/sdcard/" + fileName;
        Log.i("ExternalStorageDemo", "Save to: " + path);

        try {
            File myFile = new File(path);
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile, true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);
            myOutWriter.close();
            fOut.close();

            //Toast.makeText(getApplicationContext(), fileName + " saved", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            //e.printStackTrace();
            Log.e("fileWriter", e.getMessage());
        }
    }

    private void clearList(List list){
        list.clear();
    }
    int count;
    int x=0;
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            myEdit.setText("");
            clearList(list_device_address);
            clearList(list_rssi);
            clearList(list_to_write);
            count=0;
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    myEdit.append("\n   Total beacon count: " + count);
                    /*
                    for (int i=0;i<count;i+=1){
                        if (list_rssi.get(i)>-83)
                            list_device_address_first.add(list_device_address.get(i));
                        else if (-83 >= list_rssi.get(i) && list_rssi.get(i)>-90)
                            list_device_address_second.add(list_device_address.get(i));
                        else
                            list_device_address_far.add(list_device_address.get(i));
                    }
                    for (int i=0;i<count;i+=1){
                        if(list_rssi.get(i)==Collections.max(list_rssi)){
                            list_device_address_closest.add(list_device_address.get(i));
                        }
                    }*/
                    myEdit.append("\n"+list_device_address+"\n");
                    /*myEdit.append("\n   Adresses of the closest beacons");
                    myEdit.append(Arrays.toString(list_device_address_closest.toArray()));
                    myEdit.append("\n   Adresses of the beacons in closeness from first degree");
                    myEdit.append(Arrays.toString(list_device_address_first.toArray()));
                    myEdit.append("\n   Adresses of the beacons in closeness from second degree");
                    myEdit.append(Arrays.toString(list_device_address_second.toArray()));
                    myEdit.append("\n   Adresses of the far beacons");
                    myEdit.append(Arrays.toString(list_device_address_far.toArray()));*/

                    //for (int i=0;i<count;i+=1){
                    //    list_rssi_to_save.add(list_rssi.get(list_device_address.indexOf(list_device_address_to_save.get(i))));
                    //}

                    /*String listAdress = "";
                    for (String x : list_device_address)
                    {
                        listAdress += x + "\n";
                    }


                    String listRSSI = "";
                    for (int y : list_rssi)
                    {
                        listRSSI += y + "\n";
                    }*/


                    if(is_pressed) {
                        Long tsLong = System.currentTimeMillis();
                        String ts = tsLong.toString();
                        list_to_write.add(ts);
                        if(tag.isEmpty()==false){
                            list_to_write.add(",");
                            list_to_write.add(tag);
                        }
                        if(count>0){
                            list_to_write.add(",");
                        }
                        if(count==0){
                            list_to_write.add("\n");
                        }

                        for (int i=0;i<count;i+=1){
                            list_to_write.add(list_device_address.get(i));
                            list_to_write.add(",");
                            list_to_write.add(list_rssi.get(i).toString());
                            if(i!=count-1)
                                list_to_write.add(",");
                            if(i==count-1)
                                list_to_write.add("\n");
                        }

                        String last_list_to_write = "";
                        for (String x : list_to_write)
                        {
                            last_list_to_write += x + "";
                        }


                        writeFile(last_list_to_write,"notes.csv");




                        //writeFile(listAdress, "notes1.txt");
                        //writeFile(listRSSI, "notes2.txt");
                    }
                }
            }, SCAN_PERIOD);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(true);
                }
            }, RESTING_PERIOD);

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
                            String address=device.getAddress();
                            if (name != null) {
                                if (list_device_address.contains(address) == false && name.contains("EST") &&
                                        (address.equals("C9:00:6A:7D:EF:B8") || address.equals("C5:EC:3D:11:FB:31") || address.equals("CB:7F:3D:BD:0D:26") || address.equals("FD:15:89:12:5C:2E"))) {
                                    list_device_address.add(address);
                                    list_rssi.add(rssi);
                                    count+=1;
                                    myEdit.append(device.getName() + " " + rssi + ", count: " + count + "\n");
                                }

                            }

                        }
                    });
                }
            };




}