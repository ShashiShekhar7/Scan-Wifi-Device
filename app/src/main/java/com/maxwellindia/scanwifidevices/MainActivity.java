package com.maxwellindia.scanwifidevices;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    private WifiManager wifiManager;
    private WifiReceiver wifiReceiver;
    private Context mContext;
    WifiInfo wifiInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = MainActivity.this;

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button buttonScan = findViewById(R.id.btn_scan_device);
        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiInfo = wifiManager.getConnectionInfo();

        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (!wifiManager.isWifiEnabled()) {
//                    Toast.makeText(MainActivity.this, "Turning WiFi on", Toast.LENGTH_SHORT).show();
//                    Intent panelIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
//                    startActivityForResult(panelIntent, 0);
////                    wifiManager.setWifiEnabled(true);
//                }


                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "location turned off", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//                    Intent panelIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                    startActivityForResult(panelIntent, 0);
                } else {

                    Toast.makeText(MainActivity.this, "location turned on", Toast.LENGTH_SHORT).show();
                    wifiManager.startScan();
                }

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        ContentResolver contentResolver = this.getContentResolver();
        // Find out what the settings say about which providers are enabled
        int mode = Settings.Secure.getInt(
        contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);

        if (mode != Settings.Secure.LOCATION_MODE_OFF) {

            Toast.makeText(this, "Gps is on", Toast.LENGTH_SHORT).show();

        }else {
            Toast.makeText(this, "gps is off" , Toast.LENGTH_SHORT).show();
            Intent intent1 = new  Intent(this, LocationStateActivity.class);
            this.startActivity(intent1);
        }

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "Change network permission not granted", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CHANGE_NETWORK_STATE}, 1);

        }

        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiReceiver = new WifiReceiver(wifiManager, recyclerView, mContext);
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        registerReceiver(wifiReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));



        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "location turned off", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        }else {
            Toast.makeText(MainActivity.this, "location turned on", Toast.LENGTH_SHORT).show();

            wifiManager.startScan();
        }

    }
}