package com.maxwellindia.scanwifidevices;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static com.maxwellindia.scanwifidevices.ConfigSecurities.convertToQuotedString;


public class WifiReceiver extends BroadcastReceiver {

    WifiManager wifiManager;
    AdapterSSID adapterSSID;
    RecyclerView recyclerView;
    ScanResult mSingleScanResult;

    Context mContext;

    StringBuilder sb;

    private static final int MAX_PRIORITY = 99999;

    List<ScanResult> wifiList;


    @Nullable
    private static ConnectivityManager.NetworkCallback networkCallback;

    public WifiReceiver(WifiManager wifiManager, RecyclerView recyclerView, Context mContext) {
        this.wifiManager = wifiManager;
        this.recyclerView = recyclerView;
        this.mContext = mContext;
    }


    @Override
    public void onReceive(final Context context, Intent intent) {


        if (intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {

            ContentResolver contentResolver = context.getContentResolver();
            // Find out what the settings say about which providers are enabled
            int mode = Settings.Secure.getInt(
                    contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);

            if (mode != Settings.Secure.LOCATION_MODE_OFF) {

                Toast.makeText(context, "Gps is on", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(context, "gps is off", Toast.LENGTH_SHORT).show();
                Intent intent1 = new Intent(context, LocationStateActivity.class);
                context.startActivity(intent1);
            }

        }

        int wifiStateExtra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

        switch (wifiStateExtra) {
            case WifiManager.WIFI_STATE_ENABLED:
//                Toast.makeText(context, "Wifi on", Toast.LENGTH_SHORT).show();
                break;

            case WifiManager.WIFI_STATE_DISABLED:
//                Toast.makeText(context, "Wifi off", Toast.LENGTH_SHORT).show();
                Intent intent1 = new Intent(context, WifiStateActivity.class);
                context.startActivity(intent1);
                break;
        }

        final ArrayList<ModalSSID> ssidList = new ArrayList<>();

        String action = intent.getAction();
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            sb = new StringBuilder();

            wifiList = wifiManager.getScanResults();

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

                String password = "mx123456";
//                connectWifi(ssid, password, "WPA", context);


                if (ssid != null) {
                    mSingleScanResult = matchScanResultSsid(ssid, wifiList);
                }

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    connectToWifi(context, wifiManager, mSingleScanResult, password);
                else
                    connectWifi(ssid, password, "WPA", context);

            }
        });


    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    static boolean connectToWifi(@NonNull final Context context, @Nullable final WifiManager wifiManager, @NonNull final ScanResult scanResult, @NonNull final String password) {
        if (wifiManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return connectAndroidQ(connectivityManager, scanResult, password);
        }

        return connectPreAndroidQ(context, wifiManager, scanResult, password);
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    private static boolean connectPreAndroidQ(@NonNull final Context context, @Nullable WifiManager wifiManager, @NonNull ScanResult scanResult, @NonNull final String password) {
        if (wifiManager == null) {
            return false;
        }

        WifiConfiguration config = ConfigSecurities.getWifiConfiguration(wifiManager, scanResult);
        if (config != null && password.isEmpty()) {
            Log.d("Connect", "PASSWORD WAS EMPTY. TRYING TO CONNECT TO EXISTING NETWORK CONFIGURATION");
            return connectToConfiguredNetwork(wifiManager, config, true);
        }

        if (!cleanPreviousConfiguration(wifiManager, config)) {

            Log.d("Connect", "COULDN'T REMOVE PREVIOUS CONFIG, CONNECTING TO EXISTING ONE");

            return connectToConfiguredNetwork(wifiManager, config, true);
        }

        final String security = ConfigSecurities.getSecurity(scanResult);

        if (Objects.equals(ConfigSecurities.SECURITY_NONE, security)) {
            checkForExcessOpenNetworkAndSave(context.getContentResolver(), wifiManager);
        }

        config = new WifiConfiguration();
        config.SSID = convertToQuotedString(scanResult.SSID);
        config.BSSID = scanResult.BSSID;
        ConfigSecurities.setupSecurity(config, security, password);

        int id = wifiManager.addNetwork(config);
        Log.d("Connect", "Network ID: " + id);
        if (id == -1) {
            return false;
        }

        if (!wifiManager.saveConfiguration()) {

            Log.d("Connect", "Couldn't save wifi config");
            return false;
        }
        // We have to retrieve the WifiConfiguration after save
        config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
        if (config == null) {
            Log.d("Connect", "Error getting wifi config after save. (config == null)");
            return false;
        }

        return connectToConfiguredNetwork(wifiManager, config, true);
    }

    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE})
    private static boolean connectToConfiguredNetwork(@Nullable WifiManager wifiManager, @Nullable WifiConfiguration config, boolean reassociate) {
        if (config == null || wifiManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return disableAllButOne(wifiManager, config) && (reassociate ? wifiManager.reassociate() : wifiManager.reconnect());
        }

        // Make it the highest priority.
        int newPri = getMaxPriority(wifiManager) + 1;
        if (newPri > MAX_PRIORITY) {
            newPri = shiftPriorityAndSave(wifiManager);
            config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
            if (config == null) {
                return false;
            }
        }

        // Set highest priority to this configured network
        config.priority = newPri;
        int networkId = wifiManager.updateNetwork(config);
        if (networkId == -1) {
            return false;
        }

        // Do not disable others
        if (!wifiManager.enableNetwork(networkId, false)) {
            return false;
        }

        if (!wifiManager.saveConfiguration()) {
            return false;
        }

        // We have to retrieve the WifiConfiguration after save.
        config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
        return config != null && disableAllButOne(wifiManager, config) && (reassociate ? wifiManager.reassociate() : wifiManager.reconnect());
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private static boolean connectAndroidQ(@Nullable final ConnectivityManager connectivityManager, @NonNull ScanResult scanResult, @NonNull String password) {
        if (connectivityManager == null)
            return false;

        WifiNetworkSpecifier.Builder wifiNetworkSpecifierBuilder = new WifiNetworkSpecifier.Builder()
                .setSsid(scanResult.SSID)
                .setBssid(MacAddress.fromString(scanResult.BSSID));

        final String security = ConfigSecurities.getSecurity(scanResult);

        ConfigSecurities.setupWifiNetworkSpecifierSecurities(wifiNetworkSpecifierBuilder, security, password);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifierBuilder.build())
                .build();

        // not sure, if this is needed
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);

                Log.d("Connect", "AndroidQ+ connected to wifi ");

                // bind so all api calls are performed over this network
                connectivityManager.bindProcessToNetwork(network);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();

                Log.d("Connect", "Android Q+ could not connect to wifi");
            }
        };

        connectivityManager.requestNetwork(networkRequest, networkCallback);

        return true;

    }

    static boolean cleanPreviousConfiguration(@Nullable final WifiManager wifiManager, @Nullable final WifiConfiguration config) {
        //On Android 6.0 (API level 23) and above if my app did not create the configuration in the first place, it can not remove it either.
        if (wifiManager == null) {
            return false;
        }

        Log.d("Connect", "Attempting to remove previous network config...");
        if (config == null) {
            return true;
        }

        if (wifiManager.removeNetwork(config.networkId)) {
            wifiManager.saveConfiguration();
            return true;
        }
        return false;
    }

    private static boolean disableAllButOne(@Nullable WifiManager wifiManager, @Nullable WifiConfiguration config) {

        if (wifiManager == null) {
            return false;
        }

        @Nullable final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations == null || config == null || configurations.isEmpty()) {
            return false;
        }
        boolean result = false;

        for (WifiConfiguration wifiConfig : configurations) {
            if (wifiConfig == null) {
                continue;
            }
            if (wifiConfig.networkId == config.networkId) {
                result = wifiManager.enableNetwork(wifiConfig.networkId, true);
            } else {
                wifiManager.disableNetwork(wifiConfig.networkId);
            }
        }
        Log.d("Connect", "disableAllButOne " + result);
        return result;
    }

    private static int getMaxPriority(@Nullable final WifiManager wifiManager) {
        if (wifiManager == null) {
            return 0;
        }
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        int pri = 0;
        for (final WifiConfiguration config : configurations) {
            if (config.priority > pri) {
                pri = config.priority;
            }
        }
        return pri;
    }

    private static int shiftPriorityAndSave(@Nullable final WifiManager wifiMgr) {
        if (wifiMgr == null) {
            return 0;
        }
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);
        final int size = configurations.size();
        for (int i = 0; i < size; i++) {
            final WifiConfiguration config = configurations.get(i);
            config.priority = i;
            wifiMgr.updateNetwork(config);
        }
        wifiMgr.saveConfiguration();
        return size;
    }

    private static void sortByPriority(@NonNull final List<WifiConfiguration> configurations) {
        Collections.sort(configurations, (o1, o2) -> o1.priority - o2.priority);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean checkForExcessOpenNetworkAndSave(@NonNull final ContentResolver resolver, @NonNull final WifiManager wifiMgr) {
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);

        boolean modified = false;
        int tempCount = 0;
        final int numOpenNetworksKept = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                ? Settings.Secure.getInt(resolver, Settings.Global.WIFI_NUM_OPEN_NETWORKS_KEPT, 10)
                : Settings.Secure.getInt(resolver, Settings.Secure.WIFI_NUM_OPEN_NETWORKS_KEPT, 10);

        for (int i = configurations.size() - 1; i >= 0; i--) {
            final WifiConfiguration config = configurations.get(i);
            if (Objects.equals(ConfigSecurities.SECURITY_NONE, ConfigSecurities.getSecurity(config))) {
                tempCount++;
                if (tempCount >= numOpenNetworksKept) {
                    modified = true;
                    wifiMgr.removeNetwork(config.networkId);
                }
            }
        }
        return !modified || wifiMgr.saveConfiguration();

    }

    @Nullable
    static ScanResult matchScanResultSsid(@NonNull String ssid, @NonNull Iterable<ScanResult> results) {
        for (ScanResult result : results) {
            if (Objects.equals(result.SSID, ssid)) {
                return result;
            }
        }
        return null;
    }

    public void connectWifi(String SSID, String password, String security, Context context) {

        try {

            String networkSSID = SSID;
            String networkPass = password;


            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + networkSSID + "\"";   // Please note the quotes. String should contain ssid in quotes
            conf.status = WifiConfiguration.Status.ENABLED;
            conf.priority = 40;


            if (security.toUpperCase().contains("WEP")) {
                Log.v("rht", "Configuring WEP");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

                if (networkPass.matches("^[0-9a-fA-F]+$")) {
                    conf.wepKeys[0] = networkPass;
                } else {
                    conf.wepKeys[0] = "\"".concat(networkPass).concat("\"");
                }

                conf.wepTxKeyIndex = 0;

            } else if (security.toUpperCase().contains("WPA")) {
                Log.v("TAG", "Configuring WPA");

                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                conf.preSharedKey = "\"" + networkPass + "\"";

            } else {
                Log.v("TAG", "Configuring OPEN network");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.clear();
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            }

            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int networkId = wifiManager.addNetwork(conf);

            Log.v("TAG", "Add result " + networkId);


            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                    Log.v("TAG", "WifiConfiguration SSID " + i.SSID);


                    boolean isDisconnected = wifiManager.disconnect();
                    Log.v("TAG", "isDisconnected : " + isDisconnected);

                    boolean isEnabled = wifiManager.enableNetwork(i.networkId, true);
                    Log.v("TAG", "isEnabled : " + isEnabled);

                    boolean isReconnected = wifiManager.reconnect();
                    Log.v("TAG", "isReconnected : " + isReconnected);

                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
