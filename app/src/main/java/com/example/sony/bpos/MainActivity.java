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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.widget.Button;
import android.widget.TextView;

import android.view.View.OnClickListener;



public class MainActivity extends AppCompatActivity implements SensorEventListener, StepListener {
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
    List<String> list_device_address_all = Arrays.asList("C5:EC:3D:11:FB:31","C9:00:6A:7D:EF:B8",
            "CB:7F:3D:BD:0D:26","D2:30:33:DD:B0:4A","E0:BE:D2:07:A4:25","E6:89:33:0C:97:FB","F9:12:3C:FE:46:96","FD:15:89:12:5C:2E");
    List<String> list_device_address_all_number = Arrays.asList("B1","B2","B3","B4","B5","B6","B7","B8");


    ToggleButton logbut;
    EditText logname;
    boolean is_pressed=false;
    String tag;
    TextView TvSteps;
    Button BtnStart;
    Button BtnStop;
    boolean step_watched=false;



    //List<String> list_device_address_to_save = new ArrayList<String>(Arrays.asList ( "C9:00:6A:7D:EF:B8" , "C5:EC:3D:11:FB:31" , "CB:7F:3D:BD:0D:26", "FD:15:89:12:5C:2E" ));
    List<String> list_to_write = new ArrayList<String>();



    private static final long SCAN_PERIOD = 1000;
    private static final long RESTING_PERIOD = 2000;


    private TextView textView;
    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        TvSteps = (TextView) findViewById(R.id.tv_steps);
        BtnStart = (Button) findViewById(R.id.btn_start);
        BtnStop = (Button) findViewById(R.id.btn_stop);

        BtnStart.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                step_watched=true;
                numSteps = 0;
                sensorManager.registerListener(MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);

            }
        });


        BtnStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                step_watched=false;
                sensorManager.unregisterListener(MainActivity.this);

            }
        });





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


    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        TvSteps.setText(TEXT_NUM_STEPS + numSteps +", reference: " + reference_device_number + ", direction: " +direction);
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
    public int direction;
    int present_reference_index;
    int previous_reference_index;
    public String reference_device;
    public String reference_device_number;
    public String previous_reference;
    public String present_reference;
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
                    myEdit.append("\n"+list_device_address+"\n");
                    if(Collections.max(list_rssi)>=-78){
                        int max_rssi=Collections.max(list_rssi);
                        int max_rssi_index=list_rssi.indexOf(max_rssi);
                        reference_device=list_device_address.get(max_rssi_index);
                        reference_device_number=list_device_address_all_number.get(list_device_address_all.indexOf(reference_device));
                        numSteps=0;
                        present_reference=reference_device_number;
                        present_reference_index=list_device_address_all_number.indexOf(present_reference);
                        if((previous_reference_index+1)%list_device_address_all_number.size()==present_reference_index)
                            direction=1;
                        else if((previous_reference_index-1)%list_device_address_all_number.size()==present_reference_index)
                            direction=2;
                        else
                            direction=-1;
                        previous_reference_index=present_reference_index;
                    }




                    if(is_pressed) {
                        Long tsLong = System.currentTimeMillis();
                        String ts = tsLong.toString();
                        list_to_write.add(ts);
                        if(step_watched){
                            list_to_write.add(",");
                            list_to_write.add(""+numSteps);
                        }
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
                                        (address.equals("C9:00:6A:7D:EF:B8") || address.equals("C5:EC:3D:11:FB:31") || address.equals("CB:7F:3D:BD:0D:26") || address.equals("FD:15:89:12:5C:2E")
                                        || address.equals("D2:30:33:DD:B0:4A") || address.equals("E0:BE:D2:07:A4:25") || address.equals("F9:12:3C:FE:46:96") || address.equals("E6:89:33:0C:97:FB"))) {
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



