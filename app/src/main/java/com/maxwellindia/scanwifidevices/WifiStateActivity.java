package com.maxwellindia.scanwifidevices;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

public class WifiStateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_false);

        Intent panelIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        startActivityForResult(panelIntent, 0);
        finish();
    }
}