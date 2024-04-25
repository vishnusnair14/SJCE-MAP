package com.vishnu.sjce_map;

import android.Manifest;
import android.app.AlertDialog;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocationUpdateListener {
    private final String LOG_TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;
    private com.vishnu.sjce_map.databinding.ActivityMainBinding binding;
    private Vibrator vibrator;
    private SharedPreferences preferences;
    AlertDialog locNotEnableAlertDialog;
    AlertDialog.Builder locNotEnableBuilder;
    DocumentReference RegisteredUsersCredentialsRef;
    DocumentReference RegisteredUsersEmailRef;
    private SearchQueryListener searchQueryListener;
    HomeFragment homeFragment;
    private static LocationManager locationManager;
    SharedDataView sharedDataView;
    private GPSLocationProvider gpsLocationProvider;
    LocationModel currentLocation;
    TextView locationTV;
    TextView locNotEnaViewTV;
    FirebaseAuth mAuth;

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

        mAuth = FirebaseAuth.getInstance();
        sharedDataView = new ViewModelProvider(this).get(SharedDataView.class);
        homeFragment = new HomeFragment();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        setSupportActionBar(binding.appBarMain.toolbar);

        /* Initialize SharedPreferences */
        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        RegisteredUsersCredentialsRef = FirebaseFirestore.getInstance().collection("AuthenticationData").document("RegisteredUsersCredentials");
        RegisteredUsersEmailRef = FirebaseFirestore.getInstance().collection("AuthenticationData").document("RegisteredUsersEmail");

        locNotEnableBuilder = new AlertDialog.Builder(this);
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

        locationTV = findViewById(R.id.coordinatesViewHome_textView);
        locNotEnaViewTV = findViewById(R.id.deviceLocNotEnabledInfoView_textView);

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
                if (preferences.getBoolean("isAlreadyScanned", false)) {
                    preferences.edit().putBoolean("isAlreadyScanned", false).apply();

                    SoundNotify.playGeoFenceBoundaryExceedAlert();
                    Toast.makeText(this, "Device exceeded geofence boundary," +
                            "\nre-authentication required", Toast.LENGTH_LONG).show();
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
            preferences.edit().putBoolean("isAuthenticated", false).apply();
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