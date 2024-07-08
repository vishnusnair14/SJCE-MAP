package com.vishnu.sjcemap.ui.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.squareup.picasso.Picasso;
import com.vishnu.sjcemap.MainActivity;
import com.vishnu.sjcemap.R;
import com.vishnu.sjcemap.databinding.FragmentMapBinding;
import com.vishnu.sjcemap.miscellaneous.Overlay360View;
import com.vishnu.sjcemap.miscellaneous.SharedDataView;
import com.vishnu.sjcemap.miscellaneous.Utils;
import com.vishnu.sjcemap.service.GPSProviderService;
import com.vishnu.sjcemap.service.GeoFence;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;

public class MapFragment extends Fragment {
    SharedDataView sharedDataView;
    private static final String LOG_TAG = "MapFragment";
    DecimalFormat coordViewFormat = new DecimalFormat("#.##########");
    private String destPlaceLat;
    private String destPlaceLon;
    String gmap360ViewUrl;
    private ActivityResultLauncher<Intent> gpsActivityResultLauncher;
    Overlay360View gmap360ViewPopup;
    ConstraintLayout layout;
    private FragmentMapBinding binding;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    boolean isGpsEnabled = false;
    private double clientLat;
    private double clientLon;
    BottomSheetDialog locNotEnabledWarningBtmView;
    private final String NO_IMG_FOUND_URL = "https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/" +
            "SJCE-MAP-IMAGES%2FNO_IMAGE_FOUND_IMG.jpg?alt=media&token=ec64235b-374c-458a-aaf9-7dc67c110513";
    private final String NO_IMG_FOUND_URL_1 = "https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/" +
            "SJCE-MAP-IMAGES%2FNO_IMG_FOUND_IMG.jpg?alt=media&token=2705f83b-a59c-4def-bcd3-18600626b290";

    /* virtual location coordinates */
    private final LatLng GJB_MAIN_COORD = new LatLng(12.316369879317168, 76.61386933078991);
    private final LatLng CMS_MAIN_COORD = new LatLng(12.31780527062543, 76.61398336881378);
//    private final LatLng BACK_GATE_COORD = new LatLng(12.318453451797078, 76.61465640667447);
//    private final LatLng DUMMY_LOC_COORD = new LatLng(12.317626645254657, 76.6145323909735);

    public MapFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
        destPlaceLat = "0.0";
        destPlaceLon = "0.0";
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        layout = binding.mapFragmentLayoutConstraintLayout;
        binding.assistWalkDirectionImageButton.setEnabled(false);

        /* Get spot-place and associated data */
        sharedDataView.getPlace().observe(getViewLifecycleOwner(), pl -> {
            if (pl != null) {
                sharedDataView.getDocPath().observe(getViewLifecycleOwner(), path -> {
                    if (path != null) {
                        loadDataFromDB(path, pl, binding.coordinateViewTextView, binding.placeViewTextView);
                        Log.d(LOG_TAG, "PLACE-NAME: " + pl);
                    } else {
                        Toast.makeText(requireContext(), "docPath: NullRef", Toast.LENGTH_SHORT).show();
                        Log.i(LOG_TAG, "docPath: @null-reference");
                    }
                });
            } else {
                Toast.makeText(requireContext(), "placeName: NullRef", Toast.LENGTH_SHORT).show();
                Log.i(LOG_TAG, "placeName: @null-reference");
            }
        });

        /* updates LAT, LON TV */
        sharedDataView.getClientLat().observe(getViewLifecycleOwner(), lat -> {
            if (lat != null) {
                sharedDataView.getClientLon().observe(getViewLifecycleOwner(), lon -> {
                    if (lon != null) {
                        clientLat = lat;
                        clientLon = lon;
                    } else {
                        Toast.makeText(requireContext(), "clientLon: NullRef", Toast.LENGTH_SHORT).show();
                        Log.i(LOG_TAG, "clientLon: @null-reference");
                    }
                });
            } else {
                Toast.makeText(requireContext(), "shopLon: NullRef", Toast.LENGTH_SHORT).show();
                Log.i(LOG_TAG, "shopLon: @null-reference");
            }
        });

        // Initialize the ActivityResultLauncher
        gpsActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Utils.isGPSEnabled(requireContext())) {
                        hideLocNotEnaView();
                        setLoadingViewMapBtn();

                        new Handler().postDelayed(() -> {
                            binding.assistWalkDirectionImageButton.performClick();
                            setClickViewMapBtn();
                        }, 1000);
                        Log.d(LOG_TAG, "gpsActivityResultLauncher: GPS Enabled");
                    } else {
                        showLocNotEnaView();
                        setClickViewMapBtn();
                        Log.d(LOG_TAG, "gpsActivityResultLauncher: GPS not Enabled");
                    }
                }
        );


        return root;
    }

    private void showLocNotEnaView() {
        TextView locNotEnabledTV = binding.locNotEnabledBannerMapFragViewTextView;
        locNotEnabledTV.setVisibility(View.VISIBLE);
        locNotEnabledTV.setText(R.string.device_location_not_enabled);

        locNotEnabledTV.setTextColor(getResources().getColor(R.color.white, null));
        locNotEnabledTV.setBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
    }

    private void hideLocNotEnaView() {
        TextView locNotEnabledTV = binding.locNotEnabledBannerMapFragViewTextView;
        locNotEnabledTV.setVisibility(View.GONE);
        locNotEnabledTV.setText("");

        locNotEnabledTV.setTextColor(getResources().getColor(R.color.back_color, null));
        locNotEnabledTV.setBackgroundColor(getResources().getColor(R.color.back_color, null));
    }


    private void showLocNotEnabledWarningBtmView() {
        // Inflate the bottom sheet layout
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottomview_loc_not_enabled_warning, null, false);

        // Create the BottomSheetDialog
        locNotEnabledWarningBtmView = new BottomSheetDialog(requireContext());
        locNotEnabledWarningBtmView.setContentView(bottomSheetView);
        locNotEnabledWarningBtmView.setCanceledOnTouchOutside(false);

        bottomSheetBehavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());

        // Set a BottomSheetCallback to listen for state changes
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        Log.d(LOG_TAG, "BottomSheet state: EXPANDED");
                        // Handle the expanded state
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        Log.d(LOG_TAG, "BottomSheet state: COLLAPSED");
                        // Handle the collapsed state
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        Log.d(LOG_TAG, "BottomSheet state: DRAGGING");
                        // Handle the dragging state
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        Log.d(LOG_TAG, "BottomSheet state: SETTLING");
                        // Handle the settling state
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        Log.d(LOG_TAG, "BottomSheet state: HIDDEN");
                        setClickViewMapBtn();
                        // Handle the hidden state
                        if (locNotEnabledWarningBtmView != null) {
                            locNotEnabledWarningBtmView.dismiss();
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Handle the sliding state (slideOffset ranges from 0 to 1 as the bottom sheet slides up)
            }
        });

        Button actionButton = bottomSheetView.findViewById(R.id.button_action);

        actionButton.setOnClickListener(v -> {
            // Handle go to settings button action
            showLocationSettings();
            if (locNotEnabledWarningBtmView != null) {
                locNotEnabledWarningBtmView.hide();
                locNotEnabledWarningBtmView.dismiss();
            }
        });

        // Show the bottom sheet dialog
        if (locNotEnabledWarningBtmView != null) {
            locNotEnabledWarningBtmView.show();
        }
    }

//    private void showLocationSettings(Context context) {
//        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//        context.startActivity(intent);
//    }

    private final BroadcastReceiver gpsStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GPSProviderService.ACTION_GPS_STATUS_CHANGED.equals(intent.getAction())) {
                boolean isGPSEnabled = intent.getBooleanExtra(GPSProviderService.EXTRA_IS_GPS_ENABLED, false);

                if (isGPSEnabled && !isGpsEnabled) {
                    Toast.makeText(context, "@map: GPS Enabled", Toast.LENGTH_SHORT).show();
                    hideLocNotEnaView();

                    isGpsEnabled = true;
                } else if (!isGPSEnabled && isGpsEnabled) {
                    Toast.makeText(context, "@map: GPS Disabled", Toast.LENGTH_SHORT).show();
                    showLocNotEnaView();

                    isGpsEnabled = false;
                }
            }
        }
    };

    private void showLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        gpsActivityResultLauncher.launch(intent);
    }

    private void loadGoogleMap(String sourceLatitude, String sourceLongitude,
                               String destinationLatitude, String destinationLongitude,
                               boolean isSourceProvided, String msg) {
        Uri uri;
        if (isSourceProvided) {
            uri = Uri.parse("https://www.google.com/maps/dir/?api=1" +
                    "&origin=" + sourceLatitude + "," + sourceLongitude +
                    "&destination=" + destinationLatitude + "," + destinationLongitude +
                    "&travelmode=walking");
        } else {
            uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                    destinationLatitude + "," + destinationLongitude +
                    "&travelmode=walking");
        }

        if (MainActivity.isLocationNotEnabled(requireContext())) {
            showLocNotEnabledWarningBtmView();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, msg);

            if (locNotEnabledWarningBtmView != null) {
                locNotEnabledWarningBtmView.hide();
                locNotEnabledWarningBtmView.dismiss();
            }
        }
    }

    private void setLoadingViewMapBtn() {
        Button btn = binding.assistWalkDirectionImageButton;
        btn.setText(R.string.loading_maps);
        btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_keyboard_arrow_left_24, 0, 0, 0);
    }

    private void setClickViewMapBtn() {
        Button btn = binding.assistWalkDirectionImageButton;
        btn.setText(R.string.get_direction);
        btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_directions_walk_24, 0, 0, 0);
    }

    private void loadDataFromDB(@NonNull String docPath, @NonNull String key, TextView ltv, TextView ptv) {
        DocumentReference documentRef = FirebaseFirestore.getInstance().collection("LocationData").document(docPath);

        documentRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Retrieve data for the specified key
                    Object value = document.get(key);

                    if (value != null) {
                        if (value instanceof Map) {
                            Map<String, Object> dataMap = (Map<String, Object>) value;

                            // Extract required fields from the dataMap
                            String placeName = Objects.requireNonNull(dataMap.get("spot_name")).toString().toUpperCase();
                            String aboutDept;
                            if (dataMap.containsKey("about_department")) {
                                aboutDept = "About department\n" + Objects.requireNonNull(dataMap.get("about_department"));
                            } else if (dataMap.containsKey("about_location")) {
                                aboutDept = "About spot\n" + Objects.requireNonNull(dataMap.get("about_location"));
                            } else {
                                aboutDept = "";
                            }

                            String spotImageUrl = (String) dataMap.get("spot_image_url");
                            GeoPoint geoPoint = (GeoPoint) dataMap.get("spot_coordinates");
                            String spot360ViewUrl = (String) dataMap.get("spot_360_view_gmap_url");
                            String spotGoogleImage1 = (String) dataMap.get("spot_google_image_url_1");
                            String spotGoogleImage2 = (String) dataMap.get("spot_google_image_url_2");
                            String spotGoogleImage3 = (String) dataMap.get("spot_google_image_url_3");
                            String spotGoogleImage4 = (String) dataMap.get("spot_google_image_url_4");

                            if (Objects.equals(spot360ViewUrl, "")) {
                                layout.setVisibility(View.VISIBLE);
                                binding.viewIn360DegButton.setEnabled(false);
//                                binding.viewIn360DegButton.setVisibility(View.INVISIBLE);

                                gmap360ViewUrl = "";
                                binding.viewIn360DegButton.setOnClickListener(null);
                            } else {
                                gmap360ViewUrl = spot360ViewUrl;

                                binding.viewIn360DegButton.setVisibility(View.VISIBLE);
                                binding.viewIn360DegButton.setEnabled(true);

                                binding.viewIn360DegButton.setOnClickListener(v -> {
                                    gmap360ViewPopup = new Overlay360View(requireContext(), gmap360ViewUrl);
                                    gmap360ViewPopup.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
                                    gmap360ViewPopup.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);

                                    gmap360ViewPopup.showAtLocation(requireActivity().getWindow()
                                            .getDecorView(), Gravity.CENTER, 0, 0);
                                });
                            }
                            if (geoPoint != null) {
                                if ((geoPoint.getLatitude() == 0.00) && (geoPoint.getLongitude() == 0.00)) {
                                    binding.assistWalkDirectionImageButton.setEnabled(false);
                                    binding.assistWalkDirectionImageButton.setOnClickListener(null);
                                } else {
                                    binding.assistWalkDirectionImageButton.setEnabled(true);

                                    binding.assistWalkDirectionImageButton.setOnClickListener(v -> {
                                        setLoadingViewMapBtn();
                                        boolean isInsideGJB = GeoFence.isInsideGJB(clientLat, clientLon);
                                        boolean isInsideCMS = GeoFence.isInsideCMS(clientLat, clientLon);

                                        if (isInsideGJB) {
                                            loadGoogleMap(String.valueOf(GJB_MAIN_COORD.latitude), String.valueOf(GJB_MAIN_COORD.longitude),
                                                    destPlaceLat, destPlaceLon, true, "You are inside GJB,\nsource set from: GJB MAIN ENTRY");
                                        } else if (isInsideCMS) {
                                            loadGoogleMap(String.valueOf(CMS_MAIN_COORD.latitude), String.valueOf(CMS_MAIN_COORD.longitude),
                                                    destPlaceLat, destPlaceLon, true, "You are inside CMS,\nsource set from: CMS MAIN ENTRY");
                                        } else {
                                            loadGoogleMap(null, null, destPlaceLat, destPlaceLon, false,
                                                    "Source set from current location");

                                        }
                                    });
                                }
                            } else {
                                hideLocNotEnaView();
                                binding.assistWalkDirectionImageButton.setEnabled(false);
                                binding.assistWalkDirectionImageButton.setOnClickListener(null);
                            }

                            if (Objects.equals(spotImageUrl, "")) {
                                Picasso.get().load(NO_IMG_FOUND_URL).into(binding.imageViewMapFragCircleImageView);
                            } else {
                                Picasso.get().load(spotImageUrl).into(binding.imageViewMapFragCircleImageView);
                            }

                            /* loads destination name */
                            ptv.setText(placeName);

                            /* loads destination coordinates */
                            assert geoPoint != null;
                            ltv.setText(MessageFormat.format("{0}°N {1}°E", coordViewFormat.format(geoPoint.getLatitude()),
                                    coordViewFormat.format(geoPoint.getLongitude())));

                            /* loads destination description */
                            if (!aboutDept.isEmpty()) {
                                binding.aboutDepartmentViewTextView.setText(MessageFormat.format("{0}", aboutDept));
                            } else {
                                binding.aboutDepartmentViewTextView.setText(R.string.TODO);
                            }

                            /* loads google images */
                            if (Objects.equals(spotGoogleImage1, "")) {
                                binding.spotImageViewHorizontalScrollView.setVisibility(View.GONE);
                                binding.googleImageBannerTextView.setVisibility(View.GONE);
                            } else {
                                String[] imageUrls = {spotGoogleImage1, spotGoogleImage2, spotGoogleImage3, spotGoogleImage4};
                                ImageView[] imageViews = {binding.mapFragImageView1ImageView, binding.mapFragImageView2ImageView,
                                        binding.mapFragImageView3ImageView, binding.mapFragImageView4ImageView};

                                for (int i = 0; i < imageUrls.length; i++) {
                                    String imageUrl = imageUrls[i];
                                    ImageView imageView = imageViews[i];
                                    if (Objects.equals(imageUrl, "")) {
                                        Picasso.get().load(NO_IMG_FOUND_URL_1).into(imageView);
                                    } else {
                                        Picasso.get().load(imageUrl).into(imageView);
                                    }
                                }
                                binding.googleImageBannerTextView.setVisibility(View.VISIBLE);
                                binding.spotImageViewHorizontalScrollView.setVisibility(View.VISIBLE);
                            }

                            binding.googleImageBannerTextView.setText(MessageFormat.format("Images of {0}", placeName.toLowerCase()));

                            destPlaceLat = coordViewFormat.format(geoPoint.getLatitude());
                            destPlaceLon = coordViewFormat.format(geoPoint.getLongitude());

                        } else {
                            Log.d(LOG_TAG, "value");
                        }
                    } else {
                        Log.d(LOG_TAG, "key not found");
                        ptv.setText("");
                        ltv.setText("");
                    }
                } else {
                    Log.d(LOG_TAG, "Error fetching document: ", task.getException());
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onResume() {
        super.onResume();
//        setClickViewMapBtn();

        IntentFilter providerFilter = new IntentFilter(GPSProviderService.ACTION_GPS_STATUS_CHANGED);
        requireContext().registerReceiver(gpsStatusReceiver, providerFilter, Context.RECEIVER_NOT_EXPORTED);

        if (MainActivity.isLocationNotEnabled(requireContext())) {
            showLocNotEnaView();
//            Toast.makeText(requireContext(), "notna map", Toast.LENGTH_SHORT).show();
        } else {
            hideLocNotEnaView();
//            Toast.makeText(requireContext(), "ena map", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        requireContext().unregisterReceiver(gpsStatusReceiver);
    }
}