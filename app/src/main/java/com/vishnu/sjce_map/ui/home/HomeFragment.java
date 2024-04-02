package com.vishnu.sjce_map.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.vishnu.sjce_map.MainActivity;
import com.vishnu.sjce_map.R;
import com.vishnu.sjce_map.databinding.FragmentHomeBinding;
import com.vishnu.sjce_map.miscellaneous.SearchQueryListener;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;
import com.vishnu.sjce_map.view.SavedPlaceViewAdapter;
import com.vishnu.sjce_map.view.SavedPlaceViewModel;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment implements SearchQueryListener {
    private static final String LOG_TAG = "HomeFragment";
    public static SharedDataView sharedDataView;
    List<SavedPlaceViewModel> itemList = new ArrayList<>();
    SavedPlaceViewAdapter savedPlaceViewAdapter;
    FirebaseFirestore db;
    private FragmentHomeBinding binding;
    MainActivity mainActivity;
    GeoPoint clientGeoPoint;
    private boolean isKeyboardVisible = false;
    DecimalFormat coordinateFormat = new DecimalFormat("0.0000000000");

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

        binding.allDepartmentsShortcutButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_departmentFragment));

        syncSavedPlacesRecycleView(binding);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the search query listener
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSearchQueryListener(this);
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private void syncSavedPlacesRecycleView(@NonNull FragmentHomeBinding binding) {
        RecyclerView recyclerView = binding.spotViewRecycleView;
        LinearLayoutManager homeLayoutManager = new LinearLayoutManager(mainActivity);
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

    @Override
    public void onSearchQuerySubmitted(String query) {

    }

    @Override
    public void onSearchQueryUpdated(String query) {
        // Filter the itemList based on the search query
        List<SavedPlaceViewModel> filteredList = new ArrayList<>();
        for (SavedPlaceViewModel item : itemList) {
            if (item.getSpot_name().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        // Update the adapter with the filtered list
        savedPlaceViewAdapter.filterList(filteredList);
    }

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