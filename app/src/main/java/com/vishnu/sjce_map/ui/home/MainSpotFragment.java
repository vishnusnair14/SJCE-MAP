package com.vishnu.sjce_map.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.vishnu.sjce_map.MainActivity;
import com.vishnu.sjce_map.R;
import com.vishnu.sjce_map.databinding.FragmentHomeBinding;
import com.vishnu.sjce_map.databinding.FragmentMainSpotBinding;
import com.vishnu.sjce_map.miscellaneous.SearchQueryListener;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;
import com.vishnu.sjce_map.view.SavedPlaceViewAdapter;
import com.vishnu.sjce_map.view.SavedPlaceViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainSpotFragment extends Fragment implements SearchQueryListener {
    private static final String LOG_TAG = "MainSpotFragment";
    private FragmentMainSpotBinding binding;
    FirebaseFirestore db;
    MainActivity mainActivity;
    public static SharedDataView sharedDataView;
    List<SavedPlaceViewModel> itemList = new ArrayList<>();
    SavedPlaceViewAdapter savedPlaceViewAdapter;

    public MainSpotFragment() {
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
    private void syncSavedPlacesRecycleView(@NonNull FragmentMainSpotBinding binding) {
        RecyclerView recyclerView = binding.mainCampusSpotViewRecycleView;
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
                            itemList.clear();
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
        List<SavedPlaceViewModel> filteredList = new ArrayList<>();
        for (SavedPlaceViewModel item : itemList) {
            if (item.getSpot_name().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        savedPlaceViewAdapter.filterList(filteredList);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}