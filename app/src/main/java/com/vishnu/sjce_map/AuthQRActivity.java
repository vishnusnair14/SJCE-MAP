package com.vishnu.sjce_map;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.firestore.GeoPoint;
import com.vishnu.sjce_map.databinding.ActivityAuthQractivityBinding;
import com.vishnu.sjce_map.miscellaneous.ScanBoundaryAnim;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;
import com.vishnu.sjce_map.service.GPSLocationProvider;
import com.vishnu.sjce_map.service.LocationModel;
import com.vishnu.sjce_map.service.LocationUpdateListener;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AuthQRActivity extends AppCompatActivity implements LocationUpdateListener {
    SurfaceView surfaceView;
    TextView txtBarcodeValue;
    private LocationManager locationManager;
    SharedDataView sharedDataView;
    private GPSLocationProvider gpsLocationProvider;
    private CameraSource cameraSource;
    private static final long TIMEOUT_DURATION = 30 * 1000; // 30 seconds
    private static final long COUNTDOWN_INTERVAL = 1000; // 1 second
    private Vibrator vibrator;
    LocationModel currentLocation;
    private final String AUTH_ACCESS_KEY = "6117e11901fc7639";   // Keep it confidential
    Button byPassBtn;
    TextView locationTV;
    AlertDialog locNotEnableAlertDialog;
    DecimalFormat coordinateFormat = new DecimalFormat("0.000000000");
    private final String LOG_TAG = "AuthQRActivity";
    Intent mainActivity;
    boolean isIntentPassedToMain = false;
    boolean startActivityFlag = false;
    private long remainingTime;
    private Runnable countdownRunnable;
    private Handler timeoutHandler;
    GeoPoint SJCE_MAIN_GATE = new GeoPoint(12.313131921516486, 76.61505314496924);
    GeoPoint SJCE_EXIT_GATE = new GeoPoint(12.318439598446519, 76.61465710202344);
    private boolean alertCallFlag = false;
    AlertDialog.Builder locNotEnableBuilder;
    TextView alertTV;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    };


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_qractivity);

        sharedDataView = new ViewModelProvider(this).get(SharedDataView.class);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        locNotEnableBuilder = new AlertDialog.Builder(this);

        locNotEnableBuilder.setView(R.layout.loc_not_enable_dialog);
        locNotEnableBuilder.setPositiveButton("ENABLE", (dialog, which) -> showLocationSettings(this));
//        locNotEnableBuilder.setNegativeButton("DISABLE", (dialog, which) -> Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show());
        locNotEnableAlertDialog = locNotEnableBuilder.create();

        timeoutHandler = new Handler(Looper.getMainLooper());
        startCountdownTimer(findViewById(R.id.countDownTimer_textView));

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

        byPassBtn = findViewById(R.id.byPassAuth_button);
        locationTV = findViewById(R.id.authActivityCoordinatesView_textView);
        alertTV = findViewById(R.id.alertNotInCampus_textView);

        mainActivity = new Intent(AuthQRActivity.this, MainActivity.class);

        byPassBtn.setOnClickListener(v -> {
            startActivity(mainActivity);
            isIntentPassedToMain = true;
        });

        byPassBtn.setOnLongClickListener(v -> {
            startVibration();
            Toast.makeText(AuthQRActivity.this, "long-pressed", Toast.LENGTH_SHORT).show();
            return false;
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        currentLocation = new LocationModel(0.0000000, 0.000000);
        gpsLocationProvider = new GPSLocationProvider(this, this);

        ScanBoundaryAnim scanBoundaryAnim = findViewById(R.id.scanBoundaryAnim);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.boundary_corner_animation);
        scanBoundaryAnim.startAnimation(animation);

        startLocationUpdates();

        initViews();
    }

    private boolean isInsideGeoFenceArea(double lat, double lon) {
        // SJCE-MYSORE BACK EXIT GATE COORDINATES:
        double topLeftLat = 12.318289380014258;
        double topLeftLon = 76.61125779310221;

        // SJCE-MYSORE MAIN ENTRY GATE COORDINATES:
        double bottomRightLat = 12.311264587819064;
        double bottomRightLon = 76.61526699712476;
        return (lat >= bottomRightLat && lat <= topLeftLat && lon >= topLeftLon && lon <= bottomRightLon);
    }

    private void initViews() {
        txtBarcodeValue = findViewById(R.id.txtBarcodeValue_textView);
        surfaceView = findViewById(R.id.surfaceView);
    }

    private void initialiseDetectorsAndSources() {
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
                    Log.e("AuthQRActivity", Objects.requireNonNull(e.getMessage()));
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
                        // Check whether the user inside defined geofence-area
                        if (isInsideGeoFenceArea(currentLocation.lat, currentLocation.lon)) {
                            txtBarcodeValue.setVisibility(View.VISIBLE);
                            txtBarcodeValue.setText(R.string.qr_authenticated);
                            // Start a new activity when a match is found
                            if (!startActivityFlag) {
                                startActivity(mainActivity);
                                isIntentPassedToMain = true;
                                finish();
                            }
                            startActivityFlag = true;
                        } else {
                            runOnUiThread(() -> {
                                txtBarcodeValue.setTextColor(getColor(R.color.qr_auth_success));
                                alertTV.setText(R.string.not_in_campus);
                                txtBarcodeValue.setText(R.string.qr_authenticated);
                                startTVBlink(alertTV);
                                resetCountdownTimer();
                            });
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

    private void startTVBlink(final TextView tv) {
        if (!alertCallFlag) {
            Animation blinkAnimation = new AlphaAnimation(1, 0);
            blinkAnimation.setDuration(250);
            blinkAnimation.setRepeatMode(Animation.REVERSE);
            blinkAnimation.setRepeatCount(Animation.INFINITE);

            tv.setText(R.string.not_in_campus);
            tv.setTextColor(getColor(R.color.alert_text_red));
            tv.startAnimation(blinkAnimation);
            alertCallFlag = true;
        }
    }

    public void startLocationUpdates() {
        /* Register the listener with the Location Manager to receive location updates */
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsLocationProvider);
        } else {
            Toast.makeText(this, "Location permission disabled. Enable manually and restart the app", Toast.LENGTH_SHORT).show();
        }
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

            startVibration();
        }
    }

    private void startVibration() {
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
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

    private boolean isLocationNotEnabled(@NonNull Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Check if either GPS or network provider is enabled
        return locationManager == null ||
                (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void startCountdownTimer(TextView tv) {
        remainingTime = TIMEOUT_DURATION;

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                remainingTime -= COUNTDOWN_INTERVAL;

                tv.setText(MessageFormat.format("{0}s left...", remainingTime / 1000));
                if (remainingTime > 0) {
                    timeoutHandler.postDelayed(this, COUNTDOWN_INTERVAL);
                } else {
                    if (!isIntentPassedToMain) {
                        Toast.makeText(AuthQRActivity.this, "QR code not found.", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            }
        };

        // Post a delayed action to start the countdown
        timeoutHandler.postDelayed(countdownRunnable, COUNTDOWN_INTERVAL);
    }

    // Method to reset the countdown timer
    private void resetCountdownTimer() {
        timeoutHandler.removeCallbacks(countdownRunnable);
        startCountdownTimer(findViewById(R.id.countDownTimer_textView));
    }


    @Override
    protected void onPause() {
        super.onPause();
        cameraSource.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialiseDetectorsAndSources();

        if (isLocationNotEnabled(this)) {
            showLocNotEnableDialog(true);
        } else {
            showLocNotEnableDialog(false);
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onLocationUpdated(double latitude, double longitude) {
        updateRealTimeLoc(latitude, longitude);
        sharedDataView.setClientLat(latitude);
        sharedDataView.setClientLon(longitude);
    }
}