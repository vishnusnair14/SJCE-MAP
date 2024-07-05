package com.vishnu.sjcemap.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.vishnu.sjcemap.R;


public class LocationService extends Service {
    private final String LOG_TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    public static final String ACTION_LOCATION_BROADCAST = "LocationService.LocationBroadcast";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String ACTION_ENABLE_BROADCAST = "ACTION_ENABLE_BROADCAST";
    public static final String ACTION_DISABLE_BROADCAST = "ACTION_DISABLE_BROADCAST";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    SharedPreferences preferences;
    public static boolean isRunning = false;

    private boolean isBroadcastingEnabled = true;


    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        createNotificationChannel();
        startForeground(1, createNotification());

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (isBroadcastingEnabled) {
                        sendLocationBroadcast(location);
                        Log.d(LOG_TAG, "loc-service: loc-coords: " +
                                location.getLatitude() + "°N " + location.getLongitude() + "E°");
                    }
                }
            }
        };

        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500)
                .setMinUpdateIntervalMillis(2500)
                .setMaxUpdateDelayMillis(3000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @NonNull
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Tracking location in the background")
                .setSmallIcon(R.drawable.baseline_location_on_24)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void sendLocationBroadcast(@NonNull Location location) {
        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_LATITUDE, location.getLatitude());
        intent.putExtra(EXTRA_LONGITUDE, location.getLongitude());
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_ENABLE_BROADCAST.equals(action)) {
                isBroadcastingEnabled = true;
            } else if (ACTION_DISABLE_BROADCAST.equals(action)) {
                isBroadcastingEnabled = false;
            }
        }
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopLocationService();
        isRunning = false;
    }


    public void stopLocationService() {
        stopForeground(true);
        stopSelf();
    }
}
