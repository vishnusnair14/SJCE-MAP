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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.sjcemap.miscellaneous.SharedDataView;
import com.vishnu.sjcemap.miscellaneous.SoundNotify;
import com.vishnu.sjcemap.miscellaneous.Utils;
import com.vishnu.sjcemap.service.GeoFence;
import com.vishnu.sjcemap.service.LocationService;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
    TextView alertTV;
    private boolean isAlreadyScanned = false;
    private Handler gpsCheckHandler;
    private boolean isReceiverRegistered = false;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
    };


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_auth_qractivity);

        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts
                        .RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        // Precise location access granted.
                        Log.d(LOG_TAG, "Precise location access granted!");
                        startLocationService();
                        // Register the BroadcastReceiver
                        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
                        registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                        isReceiverRegistered = true;
                        Log.d(LOG_TAG, "Broadcast receiver registered");
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        // Only approximate location access granted.
                        startLocationService();
                        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
                        registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                        isReceiverRegistered = true;
                        Log.d(LOG_TAG, "approximate location access granted!");
                    } else {
                        // No location access granted.
                        requestPermissions();
                        Log.d(LOG_TAG, "No location access granted!");
                    }

                    if (cameraGranted != null && cameraGranted) {
                        Log.d(LOG_TAG, "camera access granted");
                    } else {
                        Log.d(LOG_TAG, "camera access not granted");
                    }
                }
        );

        // asking permission
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        sharedDataView = new ViewModelProvider(this).get(SharedDataView.class);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        locNotEnableBuilder = new AlertDialog.Builder(this);
        timeoutHandler = new Handler(Looper.getMainLooper());

        gpsCheckHandler = new Handler(Looper.getMainLooper());

        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);


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


        setStatusMsgView(R.string.checking, true);
        statusTV.setTextColor(getColor(R.color.checking));

        // Initialize the ActivityResultLauncher
        gpsActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Utils.isGPSEnabled(this)) {
                        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
                        registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                        isReceiverRegistered = true;
                        init();
                        Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();
                    } else {
//                        stopLocationService();
                        Toast.makeText(this, "GPS is not enabled", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        init();
    }


    private void requestPermissions() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Needed")
                .setMessage("Location permission is needed for core functionality")
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    setStatusMsgView(R.string.permission_required, false);
                    openAppSettings();
                })
                .create()
                .show();
    }


    public void init() {
        isAlreadyScanned = preferences.getBoolean("isAlreadyScanned", false);

        setStatusMsgView(R.string.checking, true);

        new Handler().postDelayed(() -> {
            if (!isAlreadyScanned) {
                if (isLocationNotEnabled(this)) {
                    setStatusMsgView(R.string.please_enable_yr_gps, false);
                } else {
//                    startLocationService();
                    startGpsCheckAndSetScanView();
                }
            } else {
                statusTV.setTextColor(getColor(R.color.starting_app));
                statusTV.setText(R.string.starting_app);
                new Handler().postDelayed(() -> {
                    startActivity(mainActivity);
                    finish();
                }, 500);
            }
        }, 500);
    }

    private void startGpsCheckAndSetScanView() {

        // Show a loading indicator or message to the user
//        View gpsCheckBtmView = LayoutInflater.from(this).inflate(
//                R.layout.bottomview_init_device_gps, null, false);
//
//        // Create a BottomSheetDialog with TOP gravity
//        BottomSheetDialog gpsCheckBtmDialog = new BottomSheetDialog(this);
//        gpsCheckBtmDialog.setContentView(gpsCheckBtmView);
//        gpsCheckBtmDialog.setCanceledOnTouchOutside(false);
//        Objects.requireNonNull(gpsCheckBtmDialog.getWindow()).setGravity(Gravity.TOP);
//
//        if (!gpsCheckBtmDialog.isShowing()) {
////            gpsCheckBtmDialog.show();
//        }

        // GPS coordinates are not valid, check again after the interval
        Runnable gpsCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (client_lat != 0.0 && client_lon != 0.0) {
//                    gpsCheckBtmDialog.hide();
//                    gpsCheckBtmDialog.dismiss();
                    setStatusMsgView(R.string.empty, false);
                    setScanView();
                } else {
                    // GPS coordinates are not valid, check again after the interval
                    setStatusMsgView(R.string.init_gps_please_wait, true);
                    gpsCheckHandler.postDelayed(this, GPS_CHECK_INTERVAL);
                }
            }
        };
        gpsCheckHandler.post(gpsCheckRunnable);
    }

    private void setScanView() {
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
    }

    private void setStatusMsgView(int txt, boolean showpb) {
        if (showpb) {
            statusPB.setVisibility(View.VISIBLE);
            statusTV.setVisibility(View.VISIBLE);
        } else {
            statusPB.setVisibility(View.GONE);
            statusTV.setVisibility(View.VISIBLE);
        }
        statusTV.setText(txt);
        surfaceView.setVisibility(View.GONE);
        countDownTmrTV.setVisibility(View.GONE);
        txtBarcodeValue.setVisibility(View.GONE);
        alertTV.setVisibility(View.GONE);
        locationTV.setVisibility(View.GONE);
        authBannerTV.setText(R.string.please_wait);

//        if (cameraSource != null) {
//            cameraSource.release();
//        }
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
                    } else {
                        ActivityCompat.requestPermissions(AuthQRActivity.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
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
                if (isReceiverRegistered) {
                    unregisterReceiver(locationReceiver);
                }
                Toast.makeText(getApplicationContext(), "Barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
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
                                    client_lon, testSpinner.getSelectedItem().toString())) {
                                if (!startActivityFlag) {
                                    runOnUiThread(() -> {
                                        preferences.edit().putBoolean("isAlreadyScanned", true).apply();

                                        txtBarcodeValue.setVisibility(View.VISIBLE);
                                        txtBarcodeValue.setText(R.string.qr_authenticated);
                                        alertTV.setVisibility(View.GONE);

                                        resetCountdownTimer();
                                        setStatusMsgView(R.string.starting_app, true);
                                        saveDeviceInfoToFirestore();

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
                        });
                    }
                } else {
                    txtBarcodeValue.setText(" ");
                }
            }
        });
    }

    private void saveDeviceInfoToFirestore() {
        // Get device info
        Map<String, Object> deviceInfo = DeviceInfo.getDeviceData();

        // Save device info to Firestore under a specific document
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("UserInformation").document("DeviceInfo")
                .update(deviceInfo)
                .addOnSuccessListener(aVoid -> {
                    // Handle success
                    System.out.println("DocumentSnapshot successfully written!");
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    System.err.println("Error writing document: " + e.getMessage());
                });
    }

    public static class DeviceInfo {
        public static Map<String, Object> getDeviceInfo() {
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("Manufacturer", Build.MANUFACTURER);
            deviceInfo.put("Model", Build.MODEL);
            deviceInfo.put("SDK Version", String.valueOf(Build.VERSION.SDK_INT));
            deviceInfo.put("Device", Build.DEVICE);
            deviceInfo.put("Product", Build.PRODUCT);
            return deviceInfo;
        }

        public static Map<String, Object> getDeviceData() {
            Map<String, Object> data = new HashMap<>();
            data.put(String.valueOf(new Date()), getDeviceInfo());
            return data;
        }

    }

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                client_lat = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                client_lon = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);
                Log.d("AUTH-ACT:BROADCAST-RECV:" + LOG_TAG, client_lat + "°N " + client_lon + "°E");
            } else {
                Log.d("AUTH-ACT:BROADCAST-RECV(else):" + LOG_TAG, client_lat + "°N " + client_lon + "°E");
            }
        }
    };

    private void showEnableLocationBtmView(boolean _state) {
        if (_state) {
            View enableLocationBottomView = LayoutInflater.from(this).inflate(
                    R.layout.bottomview_enable_location, null, false);

            // Create a BottomSheetDialog with TOP gravity
            enableLocBtmDialog = new BottomSheetDialog(this);
            enableLocBtmDialog.setContentView(enableLocationBottomView);
            enableLocBtmDialog.setCanceledOnTouchOutside(false);
            enableLocBtmDialog.setCancelable(false);
            Objects.requireNonNull(enableLocBtmDialog.getWindow()).setGravity(Gravity.TOP);

            Button enableLocationBtn = enableLocationBottomView.findViewById(R.id.enableDeviceLocation1_button);

            enableLocationBtn.setOnClickListener(v -> {
                enableLocBtmDialog.hide();
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


    private void startLocationService() {
        if (!LocationService.isRunning) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_ENABLE_BROADCAST);
            serviceIntent.setPackage(getPackageName());
            startForegroundService(serviceIntent);
        } else {
            Log.i(LOG_TAG, "AuthQR: Location service is already running");
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }


    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_DISABLE_BROADCAST);
        stopService(serviceIntent);
//        Toast.makeText(this, "AuthQR: location service stopped!", Toast.LENGTH_SHORT).show();
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


    private void showLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        gpsActivityResultLauncher.launch(intent);
    }

    private void startVibration() {
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.EFFECT_HEAVY_CLICK));
    }

    private boolean isLocationNotEnabled(@NonNull Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Check if either GPS or network provider is enabled
        return locationManager == null ||
                (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

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


    @Override
    protected void onPause() {
        super.onPause();

        if (cameraSource != null) {
            cameraSource.release();
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
        if (!preferences.getBoolean("isAlreadyScanned", false)) {
            showEnableLocationBtmView(isLocationNotEnabled(this));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
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
//        stopLocationService();
    }

}