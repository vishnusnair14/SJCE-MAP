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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vishnu.sjcemap.miscellaneous.SharedDataView;
import com.vishnu.sjcemap.miscellaneous.SoundNotify;
import com.vishnu.sjcemap.miscellaneous.Utils;
import com.vishnu.sjcemap.service.GeoFence;
import com.vishnu.sjcemap.service.LocationService;
import com.vishnu.sjcemap.service.MyEvent;
import com.vishnu.sjcemap.ui.authentication.AuthorizationActivity;
import com.vishnu.sjcemap.ui.authentication.HelpActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
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
    Spinner testSpinner;
    ProgressBar statusPB;
    TextView helpViewTV;
    FirebaseAuth mAuth;
    TextView authBannerTV;
    private double client_lat;
    private double client_lon;
    private boolean alertCallFlag = false;
    AlertDialog.Builder locNotEnableBuilder;
    TextView alertTV;
    FirebaseUser user;
    private boolean isAlreadyScanned = false;
    private Handler gpsCheckHandler;
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

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        sharedDataView = new ViewModelProvider(this).get(SharedDataView.class);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        locNotEnableBuilder = new AlertDialog.Builder(this);
        timeoutHandler = new Handler(Looper.getMainLooper());
        List<String> permissionsToRequest = new ArrayList<>();

        gpsCheckHandler = new Handler(Looper.getMainLooper());

        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);


        // OnCreate permission request
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
        testSpinner = findViewById(R.id.test_spinner);

        helpViewTV.setVisibility(View.GONE);

        mainActivity = new Intent(AuthQRActivity.this, MainActivity.class);
        helpActivity = new Intent(AuthQRActivity.this, HelpActivity.class);
        authenticationActivity = new Intent(AuthQRActivity.this, AuthorizationActivity.class);
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

        helpViewTV.setOnClickListener(v -> {
            startActivity(helpActivity);
            finish();
        });

        setStatusMsgView(R.string.checking, true);
        statusTV.setTextColor(getColor(R.color.checking));

        // Initialize the ActivityResultLauncher
        gpsActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Utils.isGPSEnabled(this)) {
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

    public void init() {
        isAlreadyScanned = preferences.getBoolean("isAlreadyScanned", false);
        isRemembered = preferences.getBoolean("isRemembered", false);
        isInitialLogin = preferences.getBoolean("isInitialLogin", false);
        setStatusMsgView(R.string.checking, true);

        new Handler().postDelayed(() -> {
            if (!isAlreadyScanned) {
                if (isLocationNotEnabled(this)) {
                    setStatusMsgView(R.string.please_enable_yr_gps, false);
                } else {
                    startLocationService();
                    startGpsCheckAndSetScanView();
                }
            } else {
                if (isInitialLogin || !isRemembered || user == null) {
                    startActivity(mainActivity);
                    finish();
                } else {
                    statusTV.setTextColor(getColor(R.color.signing_in));
                    statusTV.setText(R.string.signing_in);
                    new Handler().postDelayed(() -> {
                        startActivity(mainActivity);
                        finish();
                    }, 500);
                }
            }
        }, 500);
    }

    private void startGpsCheckAndSetScanView() {
        // Show a loading indicator or message to the user
        View gpsCheckBtmView = LayoutInflater.from(this).inflate(
                R.layout.init_gps_dialog, null, false);

        // Create a BottomSheetDialog with TOP gravity
        BottomSheetDialog gpsCheckBtmDialog = new BottomSheetDialog(this);
        gpsCheckBtmDialog.setContentView(gpsCheckBtmView);
        gpsCheckBtmDialog.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(gpsCheckBtmDialog.getWindow()).setGravity(Gravity.TOP);

        if (!gpsCheckBtmDialog.isShowing()) {
            gpsCheckBtmDialog.show();
        }

        // GPS coordinates are not valid, check again after the interval
        Runnable gpsCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (client_lat != 0.0 && client_lon != 0.0) {
                    gpsCheckBtmDialog.hide();
                    gpsCheckBtmDialog.dismiss();
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
        helpViewTV.setVisibility(View.VISIBLE);
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
                            if (GeoFence.isInsideGeoFenceArea(client_lat,
                                    client_lon, testSpinner.getSelectedItem().toString())) {
                                if (!startActivityFlag) {
                                    runOnUiThread(() -> {
                                        preferences.edit().putBoolean("isAlreadyScanned", true).apply();

                                        txtBarcodeValue.setVisibility(View.VISIBLE);
                                        helpViewTV.setVisibility(View.GONE);
                                        txtBarcodeValue.setText(R.string.qr_authenticated);
                                        alertTV.setVisibility(View.GONE);

                                        resetCountdownTimer();
                                        setStatusMsgView(R.string.checking, true);
                                        if (isAlreadyScanned && isInitialLogin && !isRemembered) {
                                            statusTV.setTextColor(getColor(R.color.please_wait));
                                            statusTV.setText(R.string.please_login);
                                            stopCountdownTimer();
                                            // TODO:
                                            new Handler().postDelayed(() -> {
                                                startActivity(mainActivity);
                                                finish();
                                            }, 500);
                                        } else if (!isRemembered) {
                                            statusTV.setTextColor(getColor(R.color.please_wait));
                                            statusTV.setText(R.string.please_login);
                                            stopCountdownTimer();

                                            new Handler().postDelayed(() -> {
                                                startActivity(mainActivity);
                                                finish();
                                            }, 500);
                                        } else {
                                            statusTV.setTextColor(getColor(R.color.signing_in));
                                            statusTV.setText(R.string.signing_in);
                                            stopCountdownTimer();

                                            new Handler().postDelayed(() -> {
                                                startActivity(mainActivity);
                                                finish();
                                            }, 500);
                                        }
                                        startActivityFlag = true;
                                        SoundNotify.playQRScanSuccessAlert();
                                        startVibration();
                                    });
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

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                client_lat = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                client_lon = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);
                Log.d(LOG_TAG, client_lat + " " + client_lon);
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
            startService(serviceIntent);
            Toast.makeText(this, "AuthQR: location service started!", Toast.LENGTH_SHORT).show();
        } else {
//            Toast.makeText(this, "Location service is already running", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "AuthQR: Location service is already running");
        }
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_DISABLE_BROADCAST);
        stopService(serviceIntent);
        Toast.makeText(this, "AuthQR: location service stopped!", Toast.LENGTH_SHORT).show();
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
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void showLocNotEnableDialog(boolean showFlag) {
        if (showFlag) {
            enableLocBtmDialog.show();
        } else {
            enableLocBtmDialog.hide();
            enableLocBtmDialog.cancel();
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

        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
        registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        alertTV.setVisibility(View.GONE);
        showEnableLocationBtmView(isLocationNotEnabled(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationService();
        unregisterReceiver(locationReceiver);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MyEvent event) {
        // Handle the event here
        statusTV.setText(R.string.please_wait);
        statusTV.setTextColor(getColor(R.color.please_wait));
        if (isLocationNotEnabled(this)) {
            setStatusMsgView(R.string.please_enable_yr_gps, false);
        } else {
            init();
        }
    }
}