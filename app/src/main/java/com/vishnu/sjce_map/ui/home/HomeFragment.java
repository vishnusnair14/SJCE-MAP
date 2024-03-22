package com.vishnu.sjce_map.ui.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.vishnu.sjce_map.databinding.FragmentHomeBinding;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;
import com.vishnu.sjce_map.view.SavedPlaceViewAdapter;
import com.vishnu.sjce_map.view.SavedPlaceViewModel;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {
    private static final String LOG_TAG = "HomeFragment";
    public static SharedDataView sharedDataView;
    List<SavedPlaceViewModel> itemList = new ArrayList<>();
    SavedPlaceViewAdapter savedPlaceViewAdapter;
    FirebaseFirestore db;
    private FragmentHomeBinding binding;
    GeoPoint clientGeoPoint;
    DecimalFormat coordinateFormat = new DecimalFormat("0.000000000");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
        clientGeoPoint = new GeoPoint(12.31321472272842, 76.61374613268036);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        /* updates LAT, LON TV */
        sharedDataView.getClientLat().observe(getViewLifecycleOwner(), lat -> {
            if (lat != null) {
                sharedDataView.getClientLon().observe(getViewLifecycleOwner(), lon -> {
                    if (lon != null) {
//                      clientGeoPoint = new GeoPoint(lat, lon);
                        binding.coordinatesViewHomeTextView.setText((MessageFormat.format("{0}°N\n{1}°E", coordinateFormat
                                .format(lat), coordinateFormat.format(lon))));
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

        syncSavedPlacesRecycleView(binding);
        return root;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void syncSavedPlacesRecycleView(@NonNull FragmentHomeBinding binding) {
        RecyclerView recyclerView = binding.spotViewRecycleView;
        LinearLayoutManager homeLayoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(homeLayoutManager);

        itemList.clear();

        // Add snapshot listener to listen for real-time updates
        db.collection("LocationData").document("SavedPlaceData")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> savedPlaceData = snapshot.getData();
                        if (savedPlaceData != null) {
                            itemList.clear(); // Clear itemList before updating
                            for (String field : savedPlaceData.keySet()) {
                                Map<String, Object> dataMap1 = (Map<String, Object>) savedPlaceData.get(field);
                                if (dataMap1 != null) {
                                    String itemName = (String) dataMap1.get("spot_name");
                                    GeoPoint spotCoordinates = (GeoPoint) dataMap1.get("spot_coordinates");
                                    String spotImageURL = (String) dataMap1.get("spot_image_url");
                                    String spotNameReference = (String) dataMap1.get("spot_name_reference");

                                    assert spotCoordinates != null;
                                    SavedPlaceViewModel item = new SavedPlaceViewModel(itemName, String.valueOf(spotCoordinates.getLatitude()),
                                            String.valueOf(spotCoordinates.getLongitude()), spotNameReference, spotImageURL);
                                    itemList.add(item);
                                }
                            }
                            // Update the RecyclerView adapter
                            savedPlaceViewAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.d("Firestore", "Current data: null");
                    }
                });

        savedPlaceViewAdapter = new SavedPlaceViewAdapter(itemList, requireContext(), this);
        recyclerView.setAdapter(savedPlaceViewAdapter);
    }

    public static void updatePlace(String pl, String path) {
        sharedDataView.setPlace(pl);
        sharedDataView.setDocPath(path);
    }


    private void openMapView(String sourceLatitude, String sourceLongitude,
                             String destinationLatitude, String destinationLongitude) {

        Uri uri = Uri.parse("https://www.google.com/maps/dir/" +
                sourceLatitude + "," + sourceLongitude + "/" +
                destinationLatitude + "," + destinationLongitude);

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

//    private void fetchDataFromFirestore(@NonNull String key, ValueCallback cb) {
//        DocumentReference documentRef = FirebaseFirestore.getInstance().collection("LocationData").document("SJCECampusLocations");
//
//        documentRef.get().addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
//                DocumentSnapshot document = task.getResult();
//                if (document.exists()) {
//                    // Retrieve data for the specified key
//                    Object value = document.get(key);
//
//                    if (value != null) {
//                        if (value instanceof Map) {
//                            Map<String, Object> dataMap = (Map<String, Object>) value;
//
//                            // Extract required fields from the dataMap
//                            String placeName = (String) dataMap.get("place_name");
//                            GeoPoint geoPoint = (GeoPoint) dataMap.get("coordinates");
//                            cb.onSuccess(placeName, geoPoint);
//                        } else {
//                            Log.d(LOG_TAG, "value");
//                            cb.onError("value-not-found");
//                        }
//                    } else {
//                        Log.d(LOG_TAG, "key not found");
//                        cb.onError("key-not-found");
//                    }
//                } else {
//                    Log.d("Firestore", "Error fetching document: ", task.getException());
//                    cb.onError("error-fetching-document" + task.getException());
//                }
//            }
//        });
//    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}