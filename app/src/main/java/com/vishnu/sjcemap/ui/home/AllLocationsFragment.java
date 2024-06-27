package com.vishnu.sjcemap.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.vishnu.sjcemap.MainActivity;
import com.vishnu.sjcemap.callbacks.SearchQueryListener;
import com.vishnu.sjcemap.databinding.FragmentMainSpotBinding;

import com.vishnu.sjcemap.miscellaneous.SharedDataView;
import com.vishnu.sjcemap.view.AllLocationsViewAdapter;
import com.vishnu.sjcemap.view.AllLocationsViewModel;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllLocationsFragment extends Fragment implements SearchQueryListener {
    private static final String LOG_TAG = "MainSpotFragment";
    private FragmentMainSpotBinding binding;
    private FirebaseFirestore db;
    public static SharedDataView sharedDataView;
    private List<AllLocationsViewModel> itemList = new ArrayList<>();
    private AllLocationsViewAdapter allLocationsViewAdapter;

    public AllLocationsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentMainSpotBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        syncMainCampusSpotGridView(binding);
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
    private void syncMainCampusSpotGridView(@NonNull FragmentMainSpotBinding binding) {
        GridView gridView = binding.allLocationsGridView;
        itemList.clear();

        // Add snapshot listener to listen for real-time updates
        db.collection("LocationData").document("SavedPlaceData")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(LOG_TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> savedPlaceData = snapshot.getData();
                        if (savedPlaceData != null) {
                            itemList.clear();
                            for (String field : savedPlaceData.keySet()) {
                                Map<String, Object> dataMap1 = (Map<String, Object>) savedPlaceData.get(field);
                                if (dataMap1 != null) {
                                    String itemName = (String) dataMap1.get("spot_name");
                                    GeoPoint spotCoordinates = (GeoPoint) dataMap1.get("spot_coordinates");
                                    String spotImageURL = (String) dataMap1.get("spot_image_url");
                                    String spotNameReference = (String) dataMap1.get("spot_name_reference");

                                    assert spotCoordinates != null;
                                    AllLocationsViewModel item = new AllLocationsViewModel(itemName, String.valueOf(spotCoordinates.getLatitude()),
                                            String.valueOf(spotCoordinates.getLongitude()), spotNameReference, spotImageURL);
                                    itemList.add(item);
                                }
                            }
                            // Update the GridView adapter
                            allLocationsViewAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.d(LOG_TAG, "Current data: null");
                    }
                });

        allLocationsViewAdapter = new AllLocationsViewAdapter(itemList, requireContext(), this);
        gridView.setAdapter(allLocationsViewAdapter);
    }

    public static void updatePlace(String pl, String path) {
        sharedDataView.setPlace(pl);
        sharedDataView.setDocPath(path);
    }

    @Override
    public void onSearchQuerySubmitted(String query) {
        // Implement search query submission logic if needed
    }

    @Override
    public void onSearchQueryUpdated(String query) {
        List<AllLocationsViewModel> filteredList = new ArrayList<>();
        for (AllLocationsViewModel item : itemList) {
            if (item.getSpot_name().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        allLocationsViewAdapter.filterList(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
