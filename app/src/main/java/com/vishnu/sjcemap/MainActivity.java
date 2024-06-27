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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.sjcemap.callbacks.SearchQueryListener;
import com.vishnu.sjcemap.miscellaneous.SharedDataView;
import com.vishnu.sjcemap.service.GPSProviderService;
import com.vishnu.sjcemap.service.GeoFence;
import com.vishnu.sjcemap.service.LocationService;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };
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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        locationTV = findViewById(R.id.coordinatesViewHome_textView);
        locNotEnaViewTV = findViewById(R.id.deviceLocNotEnabledInfoView_textView);
        locNotEnaViewTV.setVisibility(View.GONE);

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

        locNotEnableBuilder.setView(R.layout.loc_not_enable_dialog);
        locNotEnableBuilder.setPositiveButton("ENABLE", (dialog, which) -> showLocationSettings(this));
        locNotEnableAlertDialog = locNotEnableBuilder.create();

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        TextView emailView = headerView.findViewById(R.id.regEmailIdView_textView);

        if (mAuth.getCurrentUser() != null) {
            if (mAuth.getCurrentUser().isEmailVerified()) {
                emailView.setText(MessageFormat.format("{0} {1}",
                        mAuth.getCurrentUser().getEmail(), "(verified)"));
            } else {
                emailView.setText(mAuth.getCurrentUser().getEmail());
            }
        } else {
            emailView.setText("");
        }

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


    }


    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                client_lat = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                client_lon = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);

//                locationTV.setText(MessageFormat.format("{0}°N\n{1}°E",
//                        client_lat, client_lon));
            }
        }
    };

    private void startLocationService() {
        if (!LocationService.isRunning) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_ENABLE_BROADCAST);
            startService(serviceIntent);
            Toast.makeText(this, "MainAct: Location service started", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Location service is already running", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "Location service is already running");
        }
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_DISABLE_BROADCAST);
        stopService(serviceIntent);
        Toast.makeText(this, "MainAct: Location service stopped!", Toast.LENGTH_SHORT).show();
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

    private void executeLogoutPrefs() {
        FirebaseAuth.getInstance().signOut();
        preferences.edit().putString("username", null).apply();
        preferences.edit().putString("password", null).apply();
        preferences.edit().putBoolean("isRemembered", false).apply();
        preferences.edit().putBoolean("isAlreadyScanned", false).apply();
        preferences.edit().putBoolean("isInitialLogin", true).apply();

        startVibration();
        Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Logout", (dialog, which) -> {
            // Perform logout action
            executeLogoutPrefs();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void executeDelAccPrefs() {
        Map<String, Object> updates = new HashMap<>();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        assert user != null;
        updates.put(user.getUid().trim(), FieldValue.delete());

        user.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        preferences.edit().clear().apply();
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete account" + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });


        /* Delete user credentials from "RegisteredUsersCredentials" db bucket */
        RegisteredUsersCredentialsRef.update(updates)
                .addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Credentials deleted successfully"))
                .addOnFailureListener(e -> Log.w(LOG_TAG, "Error deleting credentials", e));

        /* Delete registered email from "RegisteredUsersEmail" db bucket */
        RegisteredUsersEmailRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> emailAddresses = (List<String>) documentSnapshot.get("email_addresses");

                if (emailAddresses != null) {
                    emailAddresses.remove(user.getEmail());

                    updates.put("email_addresses", emailAddresses);

                    // Perform the update operation
                    RegisteredUsersEmailRef.update(updates)
                            .addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Email removed successfully"))
                            .addOnFailureListener(e -> Log.w(LOG_TAG, "Error removing email", e));
                } else {
                    Log.d(LOG_TAG, "Array field 'email_addresses' is null");
                }
            } else {
                Log.d(LOG_TAG, "Document does not exist");
            }
        });

        preferences.edit().putString("username", null).apply();
        preferences.edit().putString("password", null).apply();
        preferences.edit().putBoolean("isRemembered", false).apply();
        preferences.edit().putBoolean("isAlreadyScanned", false).apply();
        preferences.edit().putBoolean("isInitialLogin", true).apply();
        finish();
    }

    public static boolean isLocationNotEnabled(@NonNull Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        /* Check if either GPS or network provider is enabled */
        return locationManager == null ||
                (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }


    private void showDelAccDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete this account forever?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Perform account-deletion action
            executeDelAccPrefs();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLocationService();
        startGPSProviderService();

        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
        registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationService();
        stopGPSProviderService();

        unregisterReceiver(locationReceiver);
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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!GeoFence.isInsideGeoFenceArea(client_lat, client_lon, "sjce")) {
            preferences.edit().putBoolean("isAuthenticated", false).apply();
            Toast.makeText(this, "Device exceeded geofence boundary,\nre-authentication required", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "isAuthenticated: False");
        }
        unregisterReceiver(locationReceiver);
        stopLocationService();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        } else if (id == R.id.action_delete_account) {
            showDelAccDialog();
        }
        return super.onOptionsItemSelected(item);
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