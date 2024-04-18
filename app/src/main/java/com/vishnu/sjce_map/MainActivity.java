package com.vishnu.sjce_map;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;
import com.vishnu.sjce_map.miscellaneous.Overlay360View;
import com.vishnu.sjce_map.miscellaneous.SearchQueryListener;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;
import com.vishnu.sjce_map.miscellaneous.SoundNotify;
import com.vishnu.sjce_map.service.GPSLocationProvider;
import com.vishnu.sjce_map.service.GeoFence;
import com.vishnu.sjce_map.service.LocationModel;
import com.vishnu.sjce_map.service.LocationUpdateListener;
import com.vishnu.sjce_map.ui.home.HomeFragment;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationUpdateListener {
    private final String LOG_TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;
    private com.vishnu.sjce_map.databinding.ActivityMainBinding binding;
    private Vibrator vibrator;
    private SharedPreferences authPreference;
    AlertDialog locNotEnableAlertDialog;
    AlertDialog.Builder locNotEnableBuilder;
    private SearchQueryListener searchQueryListener;
    HomeFragment homeFragment;
    private static LocationManager locationManager;
    SharedDataView sharedDataView;
    private GPSLocationProvider gpsLocationProvider;
    LocationModel currentLocation;
    TextView locationTV;
    TextView locNotEnaViewTV;
    private ConstraintLayout mapLayout;

    String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };

    DecimalFormat coordinateFormat = new DecimalFormat("0.000000000");

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = com.vishnu.sjce_map.databinding.ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedDataView = new ViewModelProvider(this).get(SharedDataView.class);
        homeFragment = new HomeFragment();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        setSupportActionBar(binding.appBarMain.toolbar);

        /* Initialize SharedPreferences */
        authPreference = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        locNotEnableBuilder = new AlertDialog.Builder(this);
        locNotEnableBuilder.setView(R.layout.loc_not_enable_dialog);
        locNotEnableBuilder.setPositiveButton("ENABLE", (dialog, which) -> showLocationSettings(this));

        locNotEnableAlertDialog = locNotEnableBuilder.create();

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        locationTV = findViewById(R.id.coordinatesViewHome_textView);
        locNotEnaViewTV = findViewById(R.id.deviceLocNotEnabledInfoView_textView);
        mapLayout = findViewById(R.id.mapFragmentLayout_constraintLayout);

        locNotEnaViewTV.setVisibility(View.GONE);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home, R.id.nav_about).setOpenableLayout(drawer).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // OnCreate permission request
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
        }

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        currentLocation = new LocationModel(0.0000000, 0.000000);
        gpsLocationProvider = new GPSLocationProvider(sharedDataView, this, this, locNotEnaViewTV, null);

        startLocationUpdates();

    }


    public void startLocationUpdates() {
        /* Register the listener with the Location Manager to receive location updates */
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,
                    1, gpsLocationProvider);
        } else {
            Toast.makeText(this, "location-permission-disabled", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isLocationNotEnabled(@NonNull Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        /* Check if either GPS or network provider is enabled */
        return locationManager == null ||
                (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void showLocationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    private void setCurrentLocation(double latitude, double longitude) {
        currentLocation = new LocationModel(latitude, longitude);
    }

    private LocationModel getCurrentLocation() {
        return currentLocation;
    }

    private void updateRealTimeLoc(double lat, double lon) {
        if (Math.abs(lat) > 1e-10 && Math.abs(lon) > 1e-10) {
            setCurrentLocation(lat, lon);

            locationTV.setText((MessageFormat.format("{0}°N\n{1}°E", coordinateFormat
                    .format(getCurrentLocation().lat), coordinateFormat.format(getCurrentLocation().lon))));

            if (!GeoFence.isInsideGeoFenceArea(lat, lon)) {
                if (authPreference.getBoolean("isAuthenticated", false)) {
                    authPreference.edit().putBoolean("isAuthenticated", false).apply();

                    SoundNotify.playGeoFenceBoundaryExceedNotify();
                    Toast.makeText(this, "Device exceeded geofence boundary,\nre-authentication required", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void showLocNotEnableDialog(boolean showFlag) {
        if (showFlag) {
            locNotEnableAlertDialog.setCanceledOnTouchOutside(false);
            locNotEnableAlertDialog.show();
        } else {
            locNotEnableAlertDialog.hide();
            locNotEnableAlertDialog.cancel();
        }
    }

    private void startVibration() {
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.EFFECT_DOUBLE_CLICK));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLocationNotEnabled(this)) {
            showLocNotEnableDialog(true);
            locNotEnaViewTV.setVisibility(View.VISIBLE);
        } else {
            locNotEnaViewTV.setVisibility(View.GONE);
            showLocNotEnableDialog(false);
            startLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!GeoFence.isInsideGeoFenceArea(currentLocation.lat, currentLocation.lon)) {
            authPreference.edit().putBoolean("isAuthenticated", false).apply();
            Toast.makeText(this, "Device exceeded geofence boundary,\nre-authentication required", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "isAuthenticated: False");
        }
        locationManager.removeUpdates(gpsLocationProvider);
    }

    @Override
    public void onLocationUpdated(double lat, double lon) {
        updateRealTimeLoc(lat, lon);

        sharedDataView.setClientLat(lat);
        sharedDataView.setClientLon(lon);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start geofencing
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        assert searchView != null;

        searchView.setOnCloseListener(() -> {
            findViewById(R.id.shortcutOptions_cardView).setVisibility(View.VISIBLE);
            return false;
        });

        searchView.setOnClickListener(v -> {

        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (searchQueryListener != null) {
                    Log.i(LOG_TAG, query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (searchQueryListener != null) {
                    searchQueryListener.onSearchQueryUpdated(newText);
                }
                return false;
            }
        });
        return true;
    }

    public void setSearchQueryListener(SearchQueryListener listener) {
        this.searchQueryListener = listener;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}