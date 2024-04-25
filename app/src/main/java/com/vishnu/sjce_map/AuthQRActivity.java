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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;
import com.vishnu.sjce_map.service.GPSLocationProvider;
import com.vishnu.sjce_map.service.GeoFence;
import com.vishnu.sjce_map.service.LocationModel;
import com.vishnu.sjce_map.service.LocationUpdateListener;
import com.vishnu.sjce_map.ui.authentication.AuthenticationActivity;
import com.vishnu.sjce_map.ui.authentication.HelpActivity;

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
    private SharedPreferences preferences;
    private GPSLocationProvider gpsLocationProvider;
    private CameraSource cameraSource;
    private static final long TIMEOUT_DURATION = 30 * 1000;
    private static final long COUNTDOWN_INTERVAL = 1000;
    private Vibrator vibrator;
    LocationModel currentLocation;
    private final String AUTH_ACCESS_KEY = "6117e11901fc7639";   // Keep it confidential
    Button byPassBtn;
    TextView locationTV;
    AlertDialog locNotEnableAlertDialog;
    DecimalFormat coordinateFormat = new DecimalFormat("0.000000000");
    private final String LOG_TAG = "AuthQRActivity";
    Intent mainActivity;
    Intent helpActivity;
    Intent authenticationActivity;
    Intent authQRActivity;
    boolean isIntentPassedToMain = false;
    boolean startActivityFlag = false;
    private long remainingTime;
    private Runnable countdownRunnable;
    private Handler timeoutHandler;
    CardView authScanCardView;
    TextView countDownTmrTV;
    TextView statusTV;
    ProgressBar statusPB;
    TextView helpViewTV;
    FirebaseAuth mAuth;
    TextView authBannerTV;
    private boolean alertCallFlag = false;
    AlertDialog.Builder locNotEnableBuilder;
    TextView alertTV;
    FirebaseUser user;
    private boolean isAlreadyScanned = false;
    private boolean isRemembered = false;
    private boolean isInitialLogin = false;
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

        sharedDataView = new ViewModelProvider(this).get(SharedDataView.class);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        locNotEnableBuilder = new AlertDialog.Builder(this);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        locNotEnableBuilder.setView(R.layout.loc_not_enable_dialog);
        locNotEnableBuilder.setPositiveButton("ENABLE", (dialog, which) -> showLocationSettings(this));
        locNotEnableAlertDialog = locNotEnableBuilder.create();

        timeoutHandler = new Handler(Looper.getMainLooper());

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
        authScanCardView = findViewById(R.id.authScan_cardView);
        surfaceView = findViewById(R.id.surfaceView);
        txtBarcodeValue = findViewById(R.id.txtBarcodeValue_textView);
        countDownTmrTV = findViewById(R.id.countDownTimer_textView);
        authBannerTV = findViewById(R.id.authBannerAuthQrActivity_textView);
        helpViewTV = findViewById(R.id.helpView_textView);
        statusPB = findViewById(R.id.statusPB_progressBar);
        statusTV = findViewById(R.id.statusViewAuthQRActivity_textView);

        helpViewTV.setVisibility(View.GONE);

        mainActivity = new Intent(AuthQRActivity.this, MainActivity.class);
        helpActivity = new Intent(AuthQRActivity.this, HelpActivity.class);
        authenticationActivity = new Intent(AuthQRActivity.this, AuthenticationActivity.class);
        authQRActivity = new Intent(AuthQRActivity.this, AuthQRActivity.class);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        currentLocation = new LocationModel(0.0000000000, 0.000000000);
        gpsLocationProvider = new GPSLocationProvider(sharedDataView, this, this, null, this);

        byPassBtn.setOnClickListener(v -> {
            startActivity(mainActivity);
            isIntentPassedToMain = true;
        });

        helpViewTV.setOnClickListener(v -> {
            startActivity(helpActivity);
            finish();
        });

        startLocationUpdates();
        init();
    }

    private void init() {
        setMsgView();
        statusTV.setTextColor(getColor(R.color.checking));
        statusTV.setText(R.string.checking);

        isAlreadyScanned = preferences.getBoolean("isAlreadyScanned", false);
        isRemembered = preferences.getBoolean("isRemembered", false);
        isInitialLogin = preferences.getBoolean("isInitialLogin", false);

        new Handler().postDelayed(() -> {
            if (!isAlreadyScanned) {
                setScanView();
            } else {
                if (isInitialLogin || !isRemembered || user == null) {
                    startActivity(authenticationActivity);
                    finish();
                } else {
                    statusTV.setTextColor(getColor(R.color.signing_in));
                    statusTV.setText(R.string.signing_in);
                    new Handler().postDelayed(() -> {
                        startActivity(mainActivity);
                        finish();
                    }, 1200);
                }
            }
        }, 1200);
    }


    private void setScanView() {
        initialiseScanDetectorsAndSources();
        startCountdownTimer();
        statusPB.setVisibility(View.GONE);
        statusTV.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
        helpViewTV.setVisibility(View.VISIBLE);
        countDownTmrTV.setVisibility(View.VISIBLE);
        txtBarcodeValue.setVisibility(View.VISIBLE);
        alertTV.setVisibility(View.VISIBLE);
        locationTV.setVisibility(View.VISIBLE);
        authBannerTV.setText(R.string.scan_a_valid_qr_to_authenticate);
    }

    private void setMsgView() {
        statusPB.setVisibility(View.VISIBLE);
        statusTV.setVisibility(View.VISIBLE);
        surfaceView.setVisibility(View.GONE);
        helpViewTV.setVisibility(View.GONE);
        countDownTmrTV.setVisibility(View.GONE);
        txtBarcodeValue.setVisibility(View.GONE);
        alertTV.setVisibility(View.GONE);
        locationTV.setVisibility(View.GONE);
        authBannerTV.setText(R.string.please_wait);

        if (cameraSource != null) {
            cameraSource.release();
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
                            helpViewTV.setVisibility(View.GONE);
                            txtBarcodeValue.setTextColor(getColor(R.color.enable_loc_provider));
                            txtBarcodeValue.setText(R.string.enable_your_location_provider);
                        } else {
                            // Check whether the user inside defined geofence-area
                            if (GeoFence.isInsideGeoFenceArea(currentLocation.lat, currentLocation.lon)) {
                                if (!startActivityFlag) {
                                    runOnUiThread(() -> {
                                        preferences.edit().putBoolean("isAlreadyScanned", true).apply();

                                        txtBarcodeValue.setVisibility(View.VISIBLE);
                                        helpViewTV.setVisibility(View.GONE);
                                        txtBarcodeValue.setText(R.string.qr_authenticated);
                                        alertTV.setVisibility(View.GONE);

                                        resetCountdownTimer();
                                        setMsgView();
                                        if (isAlreadyScanned && isInitialLogin && !isRemembered) {
                                            statusTV.setTextColor(getColor(R.color.please_wait));
                                            statusTV.setText(R.string.please_login);
                                            stopCountdownTimer();

                                            new Handler().postDelayed(() -> {
                                                startActivity(authenticationActivity);
                                                finish();
                                            }, 1200);
                                        } else if (!isRemembered) {
                                            statusTV.setTextColor(getColor(R.color.please_wait));
                                            statusTV.setText(R.string.please_login);
                                            stopCountdownTimer();

                                            new Handler().postDelayed(() -> {
                                                startActivity(authenticationActivity);
                                                finish();
                                            }, 1200);
                                        } else {
                                            statusTV.setTextColor(getColor(R.color.signing_in));
                                            statusTV.setText(R.string.signing_in);
                                            stopCountdownTimer();

                                            new Handler().postDelayed(() -> {
                                                startActivity(mainActivity);
                                                finish();
                                            }, 1200);
                                        }
                                        startActivityFlag = true;
                                    });
                                    startVibration();
                                }
                            } else {
                                runOnUiThread(() -> {
                                    txtBarcodeValue.setTextColor(getColor(R.color.qr_auth_success));
                                    helpViewTV.setVisibility(View.VISIBLE);
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
                            helpViewTV.setVisibility(View.GONE);
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

            tv.setVisibility(View.VISIBLE);
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

//            startVibration();
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
                        Toast.makeText(AuthQRActivity.this, "QR code not found.", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            }
        };

        // Post a delayed action to start the countdown
        timeoutHandler.postDelayed(countdownRunnable, COUNTDOWN_INTERVAL);
    }

    /* Method to reset the countdown timer */
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();

        alertTV.setVisibility(View.GONE);
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
    public void onLocationUpdated(double lat, double lon) {
        updateRealTimeLoc(lat, lon);
        sharedDataView.setClientLat(lat);
        sharedDataView.setClientLon(lon);
    }
}