package com.vishnu.sjce_map.service;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.vishnu.sjce_map.AuthQRActivity;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class GPSLocationProvider implements LocationListener {
    Context context;
    SharedDataView sharedDataView;
    private final LocationUpdateListener locationUpdateListener;
    TextView locNotEnaViewTV;
    AuthQRActivity authQRActivity;


    public GPSLocationProvider(SharedDataView sharedDataView, Context context,
                               LocationUpdateListener callback, TextView locNotEnaViewTV, AuthQRActivity authQRActivity) {
        this.context = context;
        this.locationUpdateListener = callback;
        this.locNotEnaViewTV = locNotEnaViewTV;
        this.sharedDataView = sharedDataView;
        this.authQRActivity = authQRActivity;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Handle new location updates
        locationUpdateListener.onLocationUpdated(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Handle changes in location provider status (e.g., GPS enabled/disabled)
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // Handle when the location provider is enabled
        if (locNotEnaViewTV != null) {
            locNotEnaViewTV.setVisibility(View.GONE);
            sharedDataView.setIsLocProviderEnabled(true);
        }
        if (authQRActivity != null) {
            authQRActivity.initAuth();
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        // Handle when the location provider is disabled
        if (locNotEnaViewTV != null) {
            locNotEnaViewTV.setVisibility(View.VISIBLE);
            sharedDataView.setIsLocProviderEnabled(false);
        }
    }
}
