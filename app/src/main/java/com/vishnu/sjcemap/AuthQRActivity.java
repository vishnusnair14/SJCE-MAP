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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vishnu.sjcemap.miscellaneous.SharedDataView;
import com.vishnu.sjcemap.miscellaneous.SoundNotify;
import com.vishnu.sjcemap.miscellaneous.Utils;
import com.vishnu.sjcemap.service.GeoFence;
import com.vishnu.sjcemap.service.LocationService;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Objects;

public class AuthQRActivity extends AppCompatActivity {
    SurfaceView surfaceView;
    TextView txtBarcodeValue;
    private LocationManager locationManager;
    SharedDataView sharedDataView;
    private SharedPreferences preferences;
    private CameraSource cameraSource;
    private static final long TIMEOUT_DURATION = 30 * 1000;
    private static final int GPS_CHECK_INTERVAL = 1000;
    private static final long COUNTDOWN_INTERVAL = 1000;
    private ActivityResultLauncher<Intent> gpsActivityResultLauncher;
    private Vibrator vibrator;
    private final String AUTH_ACCESS_KEY = "6117e11901fc7639";   // Keep it confidential
    Button byPassBtn;
    TextView locationTV;
    BottomSheetDialog enableLocBtmDialog;
    private final String LOG_TAG = "AuthQRActivity";
    Intent mainActivity;
    Intent authQRActivity;
    boolean isIntentPassedToMain = false;
    boolean startActivityFlag = false;
    private long remainingTime;
    private Runnable countdownRunnable;
    private Handler timeoutHandler;
    CardView authScanCardView;
    TextView countDownTmrTV;
    TextView statusTV;
    Spinner testSpinner;
    ProgressBar statusPB;
    TextView authBannerTV;
    private double client_lat;
    private double client_lon;
    private boolean alertCallFlag = false;
    AlertDialog.Builder locNotEnableBuilder;
    ActivityResultLauncher<String[]> locationPermissionRequest;
    ActivityResultLauncher<String[]> cameraPermissionRequest;
    TextView alertTV;
    private boolean isAlreadyScanned = false;
    private Handler gpsCheckHandler;
    private boolean isReceiverRegistered = false;
    boolean isFineLocationGranted = false;
    boolean isCoarseLocationGranted = false;
    boolean isCameraGranted = false;


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_auth_qractivity);
        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        sharedDataView = new ViewModelProvider(this).get(SharedDataView.class);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        locNotEnableBuilder = new AlertDialog.Builder(this);
        timeoutHandler = new Handler(Looper.getMainLooper());

        gpsCheckHandler = new Handler(Looper.getMainLooper());

        byPassBtn = findViewById(R.id.byPassAuth_button);
        locationTV = findViewById(R.id.authActivityCoordinatesView_textView);
        alertTV = findViewById(R.id.alertNotInCampus_textView);
        authScanCardView = findViewById(R.id.authScan_cardView);
        surfaceView = findViewById(R.id.surfaceView);
        txtBarcodeValue = findViewById(R.id.txtBarcodeValue_textView);
        countDownTmrTV = findViewById(R.id.countDownTimer_textView);
        authBannerTV = findViewById(R.id.authBannerAuthQrActivity_textView);
        statusPB = findViewById(R.id.statusPB_progressBar);
        statusTV = findViewById(R.id.statusViewAuthQRActivity_textView);
        testSpinner = findViewById(R.id.test_spinner);

        mainActivity = new Intent(AuthQRActivity.this, MainActivity.class);
        authQRActivity = new Intent(AuthQRActivity.this, AuthQRActivity.class);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.auth_locations,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        testSpinner.setAdapter(adapter);

        byPassBtn.setOnClickListener(v -> {
            startActivity(mainActivity);
            isIntentPassedToMain = true;
        });

        // Initialize the ActivityResultLauncher
        gpsActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Utils.isGPSEnabled(this)) {
                        setStatusMsgView(R.string.GPS_enabled, true);
                        startScannerOrRedirect();
//                        Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        showEnableLocationBtmView(true);
//                        Toast.makeText(this, "GPS is not enabled", Toast.LENGTH_SHORT).show();
                    }

                }
        );

        // registering permission results
        registerLocationPermissionResult();
        registerCameraPermissionResult();

        checkForPermissions();
    }

    private void checkForPermissions() {
        setStatusMsgView(R.string.checking_permission, true);
        isAlreadyScanned = preferences.getBoolean("isAlreadyScanned", false);
        isFineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        isCoarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        isCameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        if (!isFineLocationGranted && !isCoarseLocationGranted) {
            showProminentDisclosure("loc");
        } else if (isFineLocationGranted && isCoarseLocationGranted) {
            if (!isCameraGranted) {
                showProminentDisclosure("cam");
            } else {
                setStatusMsgView(R.string.all_permission_granted, false);
                if (!isAlreadyScanned) {
                    if (isLocationNotEnabled(this)) {
                        showEnableLocationBtmView(true);
                    } else {
                        startScannerOrRedirect();
                    }
                } else {
                    startScannerOrRedirect();
                }
            }
        }
    }

    private void registerLocationPermissionResult() {
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts
                        .RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        // Precise location access granted.
                        showProminentDisclosure("cam");
                        Log.d(LOG_TAG, "Precise location access granted!");
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        Log.d(LOG_TAG, "approximate location access granted!");
                    } else {
                        setStatusMsgView(R.string.location_permission_required, false);
                        Log.d(LOG_TAG, "No location access granted!");
                    }
                }
        );
    }

    private void registerCameraPermissionResult() {
        // asking permission
        cameraPermissionRequest = registerForActivityResult(new ActivityResultContracts
                        .RequestMultiplePermissions(), result -> {
                    Boolean cameraGranted = result.getOrDefault(
                            Manifest.permission.CAMERA, false);

                    if (cameraGranted != null && cameraGranted) {
                        if (isLocationNotEnabled(this)) {
                            showEnableLocationBtmView(true);
                        } else {
                            setStatusMsgView(R.string.ready_to_scan, false);
                            startScannerOrRedirect();
                        }
                        Log.d(LOG_TAG, "Camera access granted!");
                    } else {
                        setStatusMsgView(R.string.camera_permission_required, false);
                        Log.d(LOG_TAG, "No camera access granted!");
                    }
                }
        );
    }


    private void showProminentDisclosure(String type) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.permission_prominent_disclosure, null);
        TextView headingTxt = dialogView.findViewById(R.id.prominentDisclosureHeading_textView);
        TextView subHeadingTxt1 = dialogView.findViewById(R.id.prominentDisclosureSubPoint1_textView);
        TextView subHeadingTxt2 = dialogView.findViewById(R.id.prominentDisclosureSubPoint2_textView);
        TextView subHeadingTxt3 = dialogView.findViewById(R.id.prominentDisclosureSubPoint3_textView);

        if (preferences.getInt("permissionDenyCount", 1) >= 3) {
            if (type.equals("loc")) {
                headingTxt.setText(R.string.location_permission_required);
                subHeadingTxt1.setText(R.string.location_permission_consent_1);
                subHeadingTxt2.setText(R.string.location_permission_consent_2);
                subHeadingTxt3.setText(R.string.go_to_app_settings_app_permission);
            } else {
                headingTxt.setText(R.string.camera_permission_required);
                subHeadingTxt1.setText(R.string.cam_permission_consent_1);
                subHeadingTxt2.setText(R.string.cam_permission_consent_2);
                subHeadingTxt3.setText(R.string.go_to_app_settings_app_permission);
            }
        } else {
            if (type.equals("loc")) {
                headingTxt.setText(R.string.secure_qr_login_with_continue);
                subHeadingTxt1.setText(R.string.location_permission_consent_1);
                subHeadingTxt2.setText(R.string.location_permission_consent_2);
            } else {
                headingTxt.setText(R.string.camera_permission_required);
                subHeadingTxt1.setText(R.string.cam_permission_consent_1);
                subHeadingTxt2.setText(R.string.cam_permission_consent_2);
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setNegativeButton("Not now", (dialog, which) -> {
                    preferences.edit().putInt("permissionDenyCount", preferences.getInt("permissionDenyCount", 1) + 1).apply();
                    if (type.equals("loc")) {
                        setStatusMsgView(R.string.Permission_required,
                                R.string.location_permission_required, false);
                    } else {
                        setStatusMsgView(R.string.Permission_required,
                                R.string.camera_permission_required, false);
                    }
                    Log.d(LOG_TAG, "prominent disclosure cancelled");
                }).setPositiveButton("Continue", (dialog, which) -> {
                    if (type.equals("loc")) {
                        locationPermissionRequest.launch(new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,

                        });

                    } else {
                        cameraPermissionRequest.launch(new String[]{
                                Manifest.permission.CAMERA
                        });
                    }
                }).show();
    }

    private void setStatusMsgView(int statusTxt, boolean showpb) {
        if (showpb) {
            statusPB.setVisibility(View.VISIBLE);
            statusTV.setVisibility(View.VISIBLE);
        } else {
            statusPB.setVisibility(View.GONE);
            statusTV.setVisibility(View.VISIBLE);
        }
        statusTV.setText(statusTxt);
        surfaceView.setVisibility(View.GONE);
        countDownTmrTV.setVisibility(View.GONE);
        txtBarcodeValue.setVisibility(View.GONE);
        alertTV.setVisibility(View.GONE);
        locationTV.setVisibility(View.GONE);
        authBannerTV.setText(R.string.please_wait);
    }

    private void setStatusMsgView(int authBannerTxt, int statusTxt, boolean showpb) {
        if (showpb) {
            statusPB.setVisibility(View.VISIBLE);
            statusTV.setVisibility(View.VISIBLE);
        } else {
            statusPB.setVisibility(View.GONE);
            statusTV.setVisibility(View.VISIBLE);
        }
        statusTV.setText(statusTxt);
        surfaceView.setVisibility(View.GONE);
        countDownTmrTV.setVisibility(View.GONE);
        txtBarcodeValue.setVisibility(View.GONE);
        alertTV.setVisibility(View.GONE);
        locationTV.setVisibility(View.GONE);
        authBannerTV.setText(authBannerTxt);
    }

    private void showEnableLocationBtmView(boolean _show) {
        setStatusMsgView(R.string.waiting_for_device_loc, true);
        if (_show) {
            View enableLocationBottomView = LayoutInflater.from(this).inflate(
                    R.layout.bottomview_enable_location, null, false);

            // Create a BottomSheetDialog with TOP gravity
            enableLocBtmDialog = new BottomSheetDialog(this);
            enableLocBtmDialog.setContentView(enableLocationBottomView);
            enableLocBtmDialog.setCanceledOnTouchOutside(false);
            enableLocBtmDialog.setCancelable(false);
            Objects.requireNonNull(enableLocBtmDialog.getWindow()).setGravity(Gravity.TOP);

            Button goToSettingsBtn = enableLocationBottomView.findViewById(R.id.goToSettings_button);

            goToSettingsBtn.setOnClickListener(v -> {
                enableLocBtmDialog.hide();
                enableLocBtmDialog.dismiss();
                showLocationSettings();
            });

            if (!enableLocBtmDialog.isShowing()) {
                enableLocBtmDialog.show();
            }
        } else {
            if (enableLocBtmDialog != null) {
                enableLocBtmDialog.hide();
                enableLocBtmDialog.dismiss();
            }
        }
    }

    private void showLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        gpsActivityResultLauncher.launch(intent);
    }

    private void startLocationBroadcastInService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_ENABLE_BROADCAST);
        serviceIntent.setPackage(getPackageName());
        startForegroundService(serviceIntent);

//            Toast.makeText(this, "location-service-started", Toast.LENGTH_SHORT).show();
//            Log.i(LOG_TAG, "AuthQR: Location service is already running");
////            Toast.makeText(this, "location-service-already-running", Toast.LENGTH_SHORT).show();
//
//        }
    }

    private void stopLocationBroadcastInService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_DISABLE_BROADCAST);
        serviceIntent.setPackage(getPackageName());
        stopService(serviceIntent);
//        Toast.makeText(this, "AuthQR: location service stopped!", Toast.LENGTH_SHORT).show();
    }

    private void registerLocationBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
        registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        isReceiverRegistered = true;
        Log.d(LOG_TAG, "Broadcast receiver registered");
    }

    private boolean isLocationNotEnabled(@NonNull Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Check if either GPS or network provider is enabled
        return locationManager == null ||
                (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                client_lat = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                client_lon = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);
                Log.d(LOG_TAG, "auth-qr-broadcast-receiver: loc-coords " + client_lat + "°N " + client_lon + "°E");
            } else {
                Log.d(LOG_TAG, "auth-qr-broadcast-receiver: loc-coords " + client_lat + "°N " + client_lon + "°E");
            }
        }
    };

    private void startCountdownTimer() {
        remainingTime = TIMEOUT_DURATION;

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                remainingTime -= COUNTDOWN_INTERVAL;

                countDownTmrTV.setText(MessageFormat.format("{0}s left...", remainingTime / 1000));
                if (remainingTime > 0) {
                    timeoutHandler.postDelayed(this, COUNTDOWN_INTERVAL);
                } else {
                    if (!isIntentPassedToMain) {
                        Toast.makeText(AuthQRActivity.this,
                                "QR code not found.", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            }
        };

        // Post a delayed action to start the countdown
        timeoutHandler.postDelayed(countdownRunnable, COUNTDOWN_INTERVAL);
    }

    private void resetCountdownTimer() {
        timeoutHandler.removeCallbacks(countdownRunnable);
        startCountdownTimer();
    }

    private void stopCountdownTimer() {
        timeoutHandler.removeCallbacks(countdownRunnable);
    }

    private void startVibration() {
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.EFFECT_HEAVY_CLICK));
    }

    private void startTVBlink(final TextView tv) {
        if (!alertCallFlag) {
            Animation blinkAnimation = new AlphaAnimation(1, 0);
            blinkAnimation.setDuration(250);
            blinkAnimation.setRepeatMode(Animation.REVERSE);
            blinkAnimation.setRepeatCount(Animation.INFINITE);

            tv.setVisibility(View.VISIBLE);
            tv.setText(R.string.not_in_campus);
            tv.setTextColor(getColor(R.color.alert_text_red));
            tv.startAnimation(blinkAnimation);
            alertCallFlag = true;
        }
    }

    private void initialiseScanDetectorsAndSources() {

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(750, 750)
                .setAutoFocusEnabled(true)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(AuthQRActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    }

                } catch (IOException e) {
                    Log.e(LOG_TAG, Objects.requireNonNull(e.getMessage()));
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<>() {
            @Override
            public void release() {
//                Toast.makeText(getApplicationContext(), "Barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
                Log.i(LOG_TAG, "QR-Code scanner has been stopped");
            }

            @Override
            public void receiveDetections(@NonNull Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    String scannedData = barcodes.valueAt(0).displayValue;

                    // Compare the scanned data with the target string
                    if (scannedData.trim().equals(AUTH_ACCESS_KEY)) {
                        if (isLocationNotEnabled(AuthQRActivity.this)) {
                            txtBarcodeValue.setTextColor(getColor(R.color.enable_loc_provider));
                            txtBarcodeValue.setText(R.string.enable_your_location_provider);
                        } else {
                            // Check whether the user inside defined geofence-area
                            if (GeoFence.isInsideGeoFenceArea(client_lat,
                                    client_lon, testSpinner.getSelectedItem().toString())) { //testSpinner.getSelectedItem().toString()
                                if (!startActivityFlag) {
                                    runOnUiThread(() -> {
                                        preferences.edit().putBoolean("isAlreadyScanned", true).apply();

                                        txtBarcodeValue.setVisibility(View.VISIBLE);
                                        txtBarcodeValue.setText(R.string.qr_authenticated);
                                        alertTV.setVisibility(View.GONE);

                                        resetCountdownTimer();
                                        setStatusMsgView(R.string.starting_app, true);

                                        stopCountdownTimer();
                                        new Handler().postDelayed(() -> {
                                            startActivity(mainActivity);
                                            finish();
                                        }, 500);
                                        startActivityFlag = true;
                                        SoundNotify.playAuthSuccessAlert();
                                        startVibration();
                                    });
                                }
                            } else {
                                runOnUiThread(() -> {
                                    txtBarcodeValue.setTextColor(getColor(R.color.qr_auth_success));
                                    alertTV.setVisibility(View.VISIBLE);
                                    alertTV.setText(R.string.not_in_campus);
                                    txtBarcodeValue.setText(R.string.qr_authenticated);
                                    startTVBlink(alertTV);
                                    resetCountdownTimer();
                                    Log.d(LOG_TAG, "QR authenticated but, YOU ARE NOT INSIDE SJCE-MYSORE CAMPUS");
                                });
                            }
                        }
                    } else {
                        // No match found, update UI accordingly
                        txtBarcodeValue.post(() -> {
                            txtBarcodeValue.setTextColor(getColor(R.color.qr_auth_fail));
                            txtBarcodeValue.setText(R.string.invalid_qr);
                            alertTV.setText(" ");
                            alertCallFlag = false;
                            resetCountdownTimer();
                            Log.d(LOG_TAG, "Invalid QR");

                        });
                    }
                } else {
                    txtBarcodeValue.setText("");
                }
            }
        });
    }


    private void startScannerOrRedirect() {
        setStatusMsgView(R.string.checking, true);
        isAlreadyScanned = preferences.getBoolean("isAlreadyScanned", false);
        if (isAlreadyScanned) {
            statusTV.setTextColor(getColor(R.color.starting_app));
            setStatusMsgView(R.string.starting_app, true);

            new Handler().postDelayed(() -> {
                startActivity(mainActivity);
                finish();
            }, 500);
        } else {
            setStatusMsgView(R.string.starting_scanner, true);

            startLocationBroadcastInService();
            registerLocationBroadcastReceiver();

            // GPS coordinates are not valid, check again after the interval
            Runnable gpsCheckRunnable = new Runnable() {
                @Override
                public void run() {
                    if (client_lat != 0.0 && client_lon != 0.0) {
                        setStatusMsgView(R.string.empty, false);
                        initialiseScanDetectorsAndSources();
                        startCountdownTimer();

                        statusPB.setVisibility(View.GONE);
                        statusTV.setVisibility(View.GONE);
                        surfaceView.setVisibility(View.VISIBLE);
                        countDownTmrTV.setVisibility(View.VISIBLE);
                        txtBarcodeValue.setVisibility(View.VISIBLE);
                        alertTV.setVisibility(View.VISIBLE);
                        locationTV.setVisibility(View.VISIBLE);
                        authBannerTV.setText(R.string.scan_a_valid_qr_to_authenticate);
                    } else {
                        // GPS coordinates are not valid, check again after the interval
                        setStatusMsgView(R.string.init_gps_please_wait, true);
                        gpsCheckHandler.postDelayed(this, GPS_CHECK_INTERVAL);
                    }
                }
            };
            gpsCheckHandler.post(gpsCheckRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (cameraSource != null) {
            cameraSource.release();
            setStatusMsgView(R.string.scanner_paused, false);
        }

        if (enableLocBtmDialog != null) {
            enableLocBtmDialog.hide();
            enableLocBtmDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        alertTV.setVisibility(View.GONE);

        if (cameraSource != null) {
            setStatusMsgView(R.string.restarting_scanner, true);
            new Handler().postDelayed(this::startScannerOrRedirect, 1000);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            if (locationReceiver != null && isReceiverRegistered) {
                unregisterReceiver(locationReceiver);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        if (enableLocBtmDialog != null) {
            enableLocBtmDialog.hide();
            enableLocBtmDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationBroadcastInService();
    }
}
