package com.vishnu.sjce_map.ui.map;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.squareup.picasso.Picasso;
import com.vishnu.sjce_map.databinding.FragmentMapBinding;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;

public class MapFragment extends Fragment {
    SharedDataView sharedDataView;
    private static final String LOG_TAG = "MapFragment";
    DecimalFormat coordViewFormat = new DecimalFormat("0.0000000000000");
    private String destPlaceLat;
    private String destPlaceLon;
    private double clientLat;
    private FragmentMapBinding binding;
    private double clientLon;
    private final String NO_IMG_FOUND_URL = "https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/" +
            "SJCE-MAP-IMAGES%2FNO_IMAGE_FOUND_IMG.jpg?alt=media&token=ec64235b-374c-458a-aaf9-7dc67c110513";
    private final String NO_IMG_FOUND_URL_1 = "https://firebasestorage.googleapis.com/v0/b/sjce-map.appspot.com/o/" +
            "SJCE-MAP-IMAGES%2FNO_IMG_FOUND_IMG.jpg?alt=media&token=2705f83b-a59c-4def-bcd3-18600626b290";

    /* virtual location coordinates */
    private final LatLng GJB_MAIN_COORD = new LatLng(12.316369879317168, 76.61386933078991);
    private final LatLng CMS_MAIN_COORD = new LatLng(12.31780527062543, 76.61398336881378);
    private final LatLng BACK_GATE_COORD = new LatLng(12.318453451797078, 76.61465640667447);
    private final LatLng DUMMY_LOC_COORD = new LatLng(12.317626645254657, 76.6145323909735);

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
        destPlaceLat = "0.0000000000000";
        destPlaceLon = "0.0000000000000";
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        /* Get spot-place and associated data */
        sharedDataView.getPlace().observe(getViewLifecycleOwner(), pl -> {
            if (pl != null) {
                sharedDataView.getDocPath().observe(getViewLifecycleOwner(), path -> {
                    if (path != null) {
                        getLocData(path, pl, binding.coordinateViewTextView, binding.placeViewTextView);
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

        binding.assistWalkDirectionImageButton.setOnClickListener(v -> {
            // TODO: set client realtime-loc
            boolean isInsideGJB = isInsideGJB(clientLat, clientLon);
            boolean isInsideCMS = isInsideCMS(clientLat, clientLon);

            if (isInsideGJB) {
                openMapView(String.valueOf(GJB_MAIN_COORD.latitude), String.valueOf(GJB_MAIN_COORD.longitude),
                        destPlaceLat, destPlaceLon, true);
                Toast.makeText(requireContext(), "You are inside GJB,\nsource set from: GJB MAIN ENTRY", Toast.LENGTH_LONG).show();
                Log.i(LOG_TAG, "You are inside GJB, source set from: GJB MAIN ENTRY");
            } else if (isInsideCMS) {
                openMapView(String.valueOf(CMS_MAIN_COORD.latitude), String.valueOf(CMS_MAIN_COORD.longitude),
                        destPlaceLat, destPlaceLon, true);
                Toast.makeText(requireContext(), "You are inside CMS,\nsource set from: CMS MAIN ENTRY", Toast.LENGTH_LONG).show();
                Log.i(LOG_TAG, "You are inside CMS, source set from: CMS MAIN ENTRY");
            } else {
                openMapView(null, null, destPlaceLat, destPlaceLon, false);
            }
        });

        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    private boolean isInsideGJB(double lat, double lon) {
        // golden-jubilee-block boundary
        double topLeftLat = 12.31700026871531;
        double topLeftLon = 76.61384665081256;
        double bottomRightLat = 12.315803282353533;
        double bottomRightLon = 76.61474608292343;

        return (lat >= bottomRightLat && lat <= topLeftLat && lon >= topLeftLon && lon <= bottomRightLon);
    }


    private boolean isInsideCMS(double lat, double lon) {
        // CMS-block boundary
        double topLeftLat = 12.317812749625686;
        double topLeftLon = 76.61399819105716;
        double bottomRightLat = 12.31743221981478;
        double bottomRightLon = 76.61470260061012;

        return (lat >= bottomRightLat && lat <= topLeftLat && lon >= topLeftLon && lon <= bottomRightLon);
    }


    private void openMapView(String sourceLatitude, String sourceLongitude,
                             String destinationLatitude, String destinationLongitude, boolean isSourceProvided) {
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
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private void getLocData(@NonNull String docPath, @NonNull String key, TextView ltv, TextView ptv) {
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
                            String placeName = (String) Objects.requireNonNull(dataMap.get("spot_name")).toString().toUpperCase();
                            String spotImageUrl = (String) dataMap.get("spot_image_url");
                            String spotGoogleImage1 = (String) dataMap.get("spot_google_image_url_1");
                            String spotGoogleImage2 = (String) dataMap.get("spot_google_image_url_2");
                            String spotGoogleImage3 = (String) dataMap.get("spot_google_image_url_3");
                            String spotGoogleImage4 = (String) dataMap.get("spot_google_image_url_4");
                            GeoPoint geoPoint = (GeoPoint) dataMap.get("spot_coordinates");

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

                            /* loads google images */
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

                            binding.googleImageBannerTextView.setText(MessageFormat.format("Google Images of {0}", placeName));

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
                    Log.d("Firestore", "Error fetching document: ", task.getException());
//                    callback.onError("Error fetching document: " + task.getException().getMessage());
                }
            }
        });
    }

}