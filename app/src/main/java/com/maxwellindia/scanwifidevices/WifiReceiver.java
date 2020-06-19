package com.maxwellindia.scanwifidevices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class WifiReceiver extends BroadcastReceiver {

    WifiManager wifiManager;
    AdapterSSID adapterSSID;
    RecyclerView recyclerView;

    Context mContext;


    StringBuilder sb;

    public WifiReceiver(WifiManager wifiManager, RecyclerView recyclerView, Context mContext) {
        this.wifiManager = wifiManager;
        this.recyclerView = recyclerView;
        this.mContext = mContext;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        final ArrayList<ModalSSID> ssidList = new ArrayList<>();

        String action = intent.getAction();
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            sb = new StringBuilder();

            List<ScanResult> wifiList = wifiManager.getScanResults();

            for (ScanResult scanResult : wifiList) {
                sb.append(scanResult.SSID);
                ModalSSID ssid = new ModalSSID(scanResult.SSID);
                ssidList.add(ssid);
                Log.d("TAG", "onReceive: " + sb);
            }

        }

        adapterSSID = new AdapterSSID(context, ssidList);

        recyclerView.setAdapter(adapterSSID);

        adapterSSID.setOnItemClickListener(new AdapterSSID.OnItemClickListener() {
            @Override
            public void onItemClick(int positon) {
                final int pos = positon;

                String ssid = ssidList.get(pos).getSsid();

                Toast.makeText(mContext, ssid, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
