package com.vishnu.sjce_map.ui.map;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.vishnu.sjce_map.R;
import com.vishnu.sjce_map.databinding.FragmentMapBinding;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Map;

public class MapFragment extends Fragment {
    SharedDataView sharedDataView;
    private static final String LOG_TAG = "MapFragment";
    DecimalFormat coordViewFormat = new DecimalFormat("0.0000000000000");
    private String destPlaceLat;
    private String destPlaceLon;
    private double clientLat;
    private FragmentMapBinding binding;
    private double clientLon;
    private final LatLng GJB_MAIN_COORD = new LatLng(12.316369879317168, 76.61386933078991);
    private final LatLng CMS_MAIN_COORD = new LatLng(12.31780527062543, 76.61398336881378);
    private final LatLng BACK_GATE_COORD = new LatLng(12.318453451797078, 76.61465640667447);
    private final LatLng DUMMY_LOC_COORD = new LatLng(12.317626645254657, 76.6145323909735 );

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
//                Log.d(LOG_TAG,"PLACE-NAME: "+ pl);
//                Toast.makeText(requireContext(), pl, Toast.LENGTH_SHORT).show();
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
                        binding.startPlaceCoordinatesViewTextView.setText((MessageFormat.format("{0}°N\n{1}°E", coordViewFormat
                                .format(lat), coordViewFormat.format(lon))));
                        binding.startPlaceNameViewTextView.setText(R.string.current_location);
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
            boolean isInsideGJB = isInsideGJB(DUMMY_LOC_COORD.latitude, DUMMY_LOC_COORD.longitude);
            boolean isInsideCMS = isInsideCMS(DUMMY_LOC_COORD.latitude, DUMMY_LOC_COORD.longitude);

            if (isInsideGJB) {
                openMapView(String.valueOf(GJB_MAIN_COORD.latitude), String.valueOf(GJB_MAIN_COORD.longitude),
                        destPlaceLat, destPlaceLon, true);
            } else if (isInsideCMS) {
                openMapView(String.valueOf(CMS_MAIN_COORD.latitude), String.valueOf(CMS_MAIN_COORD.longitude),
                        destPlaceLat, destPlaceLon, true);
            } else {
                openMapView(null, null, destPlaceLat, destPlaceLon, false);
                Toast.makeText(requireContext(), "NOT INSIDE GJB", Toast.LENGTH_SHORT).show();
            }
        });

        binding.changeStartTypeTextView.setOnClickListener(v -> Toast.makeText(requireContext(), "change-start-type", Toast.LENGTH_SHORT).show());
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
                             String destinationLatitude, String destinationLongitude, boolean isSourceSet) {
        Uri uri;
        if (isSourceSet) {
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
                            String placeName = (String) dataMap.get("spot_name");
                            GeoPoint geoPoint = (GeoPoint) dataMap.get("spot_coordinates");
                            ptv.setText(placeName);
                            binding.endPlaceNameViewTextView.setText(placeName);
                            assert geoPoint != null;
                            binding.endPlaceCoordinatesViewTextView.setText(MessageFormat.format("{0}°N\n{1}°E",
                                    coordViewFormat.format(geoPoint.getLatitude()), coordViewFormat.format(geoPoint.getLongitude())));
                            ltv.setText(MessageFormat.format("{0}°N {1}°E", coordViewFormat.format(geoPoint.getLatitude()),
                                    coordViewFormat.format(geoPoint.getLongitude())));

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