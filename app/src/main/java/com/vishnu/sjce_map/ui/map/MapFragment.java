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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
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
    DecimalFormat coordinatesFormat = new DecimalFormat("0.0000000000000");
    private GoogleMap mMap;
    private String destPlaceLat;
    private String destPlaceLon;
    MapView mapView;
    private static final LatLng BACK_GATE = new LatLng(12.318479671291703, 76.6145840732339);

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

    private FragmentMapBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        sharedDataView.getPlace().observe(getViewLifecycleOwner(), pl -> {
            if (pl != null) {
                sharedDataView.getDocPath().observe(getViewLifecycleOwner(), path -> {
                    if (path != null) {
                        fetchDataFromFirestore(path, pl, binding.coordinateViewTextView, binding.placeViewTextView);
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
                        binding.startPlaceCoordinatesViewTextView.setText((MessageFormat.format("{0}°N\n{1}°E", coordinatesFormat
                                .format(lat), coordinatesFormat.format(lon))));
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

        binding.assistWalkDirectionImageButton.setOnClickListener(v -> openMapView(String.valueOf(BACK_GATE.latitude),
                String.valueOf(BACK_GATE.longitude), destPlaceLat, destPlaceLon));

        binding.changeStartTypeTextView.setOnClickListener(v -> Toast.makeText(requireContext(), "change-start-type", Toast.LENGTH_SHORT).show());
        return root;
    }


    private void openMapView(String sourceLatitude, String sourceLongitude,
                             String destinationLatitude, String destinationLongitude) {

        Uri uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                destinationLatitude + "," + destinationLongitude +
                "&travelmode=walking");

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void fetchDataFromFirestore(@NonNull String docPath, @NonNull String key, TextView
            ltv, TextView ptv) {
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
                                    coordinatesFormat.format(geoPoint.getLatitude()), coordinatesFormat.format(geoPoint.getLongitude())));
                            ltv.setText(MessageFormat.format("{0}°N {1}°E", coordinatesFormat.format(geoPoint.getLatitude()),
                                    coordinatesFormat.format(geoPoint.getLongitude())));

                            destPlaceLat = coordinatesFormat.format(geoPoint.getLatitude());
                            destPlaceLon = coordinatesFormat.format(geoPoint.getLongitude());

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