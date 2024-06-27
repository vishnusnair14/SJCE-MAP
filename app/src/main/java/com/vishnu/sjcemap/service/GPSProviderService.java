package com.vishnu.sjcemap.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.IBinder;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import java.util.Objects;

public class GPSProviderService extends Service {

    private boolean isGpsEnabled = false;
    public static boolean isRunning = false;
    public static final String ACTION_GPS_STATUS_CHANGED = "com.vishnu.sjcemap.GPS_STATUS_CHANGED";
    public static final String EXTRA_IS_GPS_ENABLED = "isGpsEnabled";

    private final BroadcastReceiver gpsProviderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.requireNonNull(intent.getAction()).matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

                if (isGPSEnabled && !isGpsEnabled) {
                    // GPS enabled and toast hasn't been shown before
//                    Toast.makeText(context, "GPS Enabled", Toast.LENGTH_SHORT).show();
                    isGpsEnabled = true;
                } else if (!isGPSEnabled && isGpsEnabled) {
                    // GPS disabled and toast hasn't been shown before
//                    Toast.makeText(context, "GPS Disabled", Toast.LENGTH_SHORT).show();
                    isGpsEnabled = false;
                }

                // Send broadcast
                Intent gpsStatusIntent = new Intent(ACTION_GPS_STATUS_CHANGED);
                gpsStatusIntent.putExtra(EXTRA_IS_GPS_ENABLED, isGpsEnabled);
                sendBroadcast(gpsStatusIntent);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(gpsProviderReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        unregisterReceiver(gpsProviderReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
