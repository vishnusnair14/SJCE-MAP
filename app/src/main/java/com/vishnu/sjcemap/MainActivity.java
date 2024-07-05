package com.vishnu.sjcemap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.sjcemap.callbacks.SearchQueryListener;
import com.vishnu.sjcemap.miscellaneous.SharedDataView;
import com.vishnu.sjcemap.service.GPSProviderService;
import com.vishnu.sjcemap.service.GeoFence;
import com.vishnu.sjcemap.service.LocationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static LocationManager locationManager;
    private final String LOG_TAG = "MainActivity";
    AlertDialog locNotEnableAlertDialog;
    AlertDialog.Builder locNotEnableBuilder;
    DocumentReference RegisteredUsersCredentialsRef;
    DocumentReference RegisteredUsersEmailRef;
    SharedDataView sharedDataView;
    TextView locationTV;
    private double client_lat;
    private double client_lon;
    TextView locNotEnaViewTV;
    FirebaseFirestore db;

    private AppBarConfiguration mAppBarConfiguration;
    private com.vishnu.sjcemap.databinding.ActivityMainBinding binding;
    private Vibrator vibrator;
    private SharedPreferences preferences;
    private SearchQueryListener searchQueryListener;


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = com.vishnu.sjcemap.databinding.ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        db = FirebaseFirestore.getInstance();

        locationTV = findViewById(R.id.coordinatesViewHome_textView);
        locNotEnaViewTV = findViewById(R.id.deviceLocNotEnabledInfoView_textView);
        locNotEnaViewTV.setVisibility(View.GONE);

        // Register the receiver
        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
        registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        sharedDataView = new ViewModelProvider(this).get(SharedDataView.class);

        Intent mainNavBarBtmTxtOnClickLinkViewIntent = new Intent(Intent.ACTION_VIEW);
        locNotEnableBuilder = new AlertDialog.Builder(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        DocumentReference dr = db.collection("DeveloperData").document("AppData");

        RegisteredUsersCredentialsRef = db.collection("AuthenticationData")
                .document("RegisteredUsersCredentials");
        RegisteredUsersEmailRef = db.collection("AuthenticationData")
                .document("RegisteredUsersEmail");

//        locNotEnableBuilder.setView(R.layout.loc_not_enable_dialog);
//        locNotEnableBuilder.setPositiveButton("ENABLE", (dialog, which) -> showLocationSettings(this));
//        locNotEnableAlertDialog = locNotEnableBuilder.create();

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        TextView navBtmBannerTV = findViewById(R.id.textView10);

        navBtmBannerTV.setOnClickListener(v -> {
            try {
                startActivity(mainNavBarBtmTxtOnClickLinkViewIntent);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            }
        });

        /* listener for: mainNavigationBarBottomTextOnClickLink */
        dr.addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                if (snapshot.contains("mainNavigationBarBottomTextOnClickLink")) {
                    if (!Objects.requireNonNull(snapshot.get("mainNavigationBarBottomTextOnClickLink")).toString().isEmpty()) {
                        mainNavBarBtmTxtOnClickLinkViewIntent.setData(Uri.parse((String)
                                snapshot.get("mainNavigationBarBottomTextOnClickLink")));
                    } else {
                        mainNavBarBtmTxtOnClickLinkViewIntent.setData(Uri.parse("https://sjce.ac.in"));
                    }
                }
            }
        });

        /* listener for: mainNavigationBarBottomText */
        dr.addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                if (snapshot.contains("mainNavigationBarBottomBannerText")) {
                    navBtmBannerTV.setText((String) snapshot.get("mainNavigationBarBottomBannerText"));
                } else {
                    navBtmBannerTV.setText(R.string.sjce_map);
                }
            }
        });

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home, R.id.nav_about).setOpenableLayout(drawer).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

    }


    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                client_lat = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                client_lon = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);

//                locationTV.setText(MessageFormat.format("{0}°N\n{1}°E",
//                        client_lat, client_lon));

                if (!GeoFence.isInsideGeoFenceArea(client_lat, client_lon, "GKLM")) {
                    preferences.edit().putBoolean("isAuthenticated", false).apply();
                    Toast.makeText(context, "Device exceeded SJCE boundary, re-authentication required", Toast.LENGTH_SHORT).show();
                    Log.i(LOG_TAG, "isAuthenticated: False");
                }
                Log.d("MAIN-ACT:BROADCAST-RECV:" + LOG_TAG, client_lat + "°N " + client_lon + "°E");

            }
        }
    };

    private void startLocationService() {
        if (!LocationService.isRunning) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_ENABLE_BROADCAST);
            serviceIntent.setPackage(getPackageName());
            startService(serviceIntent);
//            Toast.makeText(this, "MainAct: Location service started", Toast.LENGTH_SHORT).show();
        } else {
//            Toast.makeText(this, "Location service is already running", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "Location service is already running");
        }
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_DISABLE_BROADCAST);
        serviceIntent.setPackage(getPackageName());
        stopService(serviceIntent);
//        Toast.makeText(this, "MainAct: Location service stopped!", Toast.LENGTH_SHORT).show();
    }

    private void startGPSProviderService() {
        Intent serviceIntent = new Intent(this, GPSProviderService.class);
        startService(serviceIntent);
    }

    private void stopGPSProviderService() {
        Intent serviceIntent = new Intent(this, GPSProviderService.class);
        stopService(serviceIntent);
    }


    private void showLocationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }


    private void showLocNotEnableDialog(boolean showFlag) {

    }

    private void startVibration() {
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.EFFECT_DOUBLE_CLICK));
    }


    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset");
        builder.setMessage("Are you sure you want to reset. This will remove all your saved preferences?");
        builder.setPositiveButton("Reset", (dialog, which) -> {
            // Perform reset action
            preferences.edit().putBoolean("isAlreadyScanned", false).apply();
            startVibration();
            Toast.makeText(this, "Reset successful", Toast.LENGTH_SHORT).show();

            finish();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public static boolean isLocationNotEnabled(@NonNull Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        /* Check if either GPS or network provider is enabled */
        return locationManager == null ||
                (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationService();
        startGPSProviderService();

        if (isLocationNotEnabled(this)) {
            if (!preferences.getBoolean("isAlreadyScanned", false)) {
                showLocNotEnableDialog(true);
                locNotEnaViewTV.setVisibility(View.VISIBLE);
            }
        } else {
            locNotEnaViewTV.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!GeoFence.isInsideGeoFenceArea(client_lat, client_lon, "GKLM")) {
            preferences.edit().putBoolean("isAuthenticated", false).apply();
            Toast.makeText(this, "Device exceeded SJCE boundary, re-authentication required", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "isAuthenticated: False");
        }
        try {
            unregisterReceiver(locationReceiver);
            stopLocationService();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationService();
        stopGPSProviderService();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            // Handle settings menu item click
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setSearchQueryListener(SearchQueryListener listener) {
        this.searchQueryListener = listener;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this,
                R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

}