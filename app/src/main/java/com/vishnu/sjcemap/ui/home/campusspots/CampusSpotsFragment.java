package com.vishnu.sjcemap.ui.home.campusspots;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.vishnu.sjcemap.MainActivity;
import com.vishnu.sjcemap.callbacks.SearchQueryListener;
import com.vishnu.sjcemap.databinding.FragmentMainSpotBinding;

import com.vishnu.sjcemap.miscellaneous.SharedDataView;


import java.util.ArrayList;
import java.util.List;

public class CampusSpotsFragment extends Fragment implements SearchQueryListener {
    private static final String LOG_TAG = "MainSpotFragment";
    private FragmentMainSpotBinding binding;
    private FirebaseFirestore db;
    public static SharedDataView sharedDataView;
    private List<CampusSpotsModel> itemList;
    List<CampusSpotsModel> filteredList;
    private CampusSpotsAdapter campusSpotsAdapter;
    GridView gridView;
    CollectionReference ordersRef;

    public CampusSpotsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
//        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
        itemList = new ArrayList<>();
        filteredList = new ArrayList<>();
        ordersRef = FirebaseFirestore.getInstance()
                .collection("AllLocations").document("allMainCampusLocations")
                .collection("data");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentMainSpotBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        gridView = binding.allLocationsGridView;
        campusSpotsAdapter = new CampusSpotsAdapter(itemList, requireContext(), this);
        gridView.setAdapter(campusSpotsAdapter);

        syncAllDepartmentsGridView();
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
    private void syncAllDepartmentsGridView() {
        binding.campusSpotGridViewProgressBar.setVisibility(View.VISIBLE);


        ordersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null && !task.getResult().isEmpty()) {
                    itemList.clear();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        if (doc.exists()) {
                            Log.d(LOG_TAG, "All main locations data: " + doc.getData());
                            CampusSpotsModel order = doc.toObject(CampusSpotsModel.class);
                            itemList.add(order);
                        }
                    }
                    campusSpotsAdapter.notifyDataSetChanged();
                    binding.campusSpotGridViewProgressBar.setVisibility(View.GONE);
                } else {
                    Log.d(LOG_TAG, "Current data: null or empty");
                    binding.campusSpotGridViewProgressBar.setVisibility(View.GONE);
                }
            } else {
                Log.w(LOG_TAG, "Error getting documents.", task.getException());
                binding.campusSpotGridViewProgressBar.setVisibility(View.GONE);
            }
        });
    }

//    @SuppressLint("NotifyDataSetChanged")
//    private void syncMainCampusSpotGridView(@NonNull FragmentMainSpotBinding binding) {
//        GridView gridView = binding.allLocationsGridView;
//        itemList.clear();
//
//        // Add snapshot listener to listen for real-time updates
//        db.collection("LocationData").document("SavedPlaceData")
//                .addSnapshotListener((snapshot, e) -> {
//                    if (e != null) {
//                        Log.w(LOG_TAG, "Listen failed.", e);
//                        return;
//                    }
//
//                    if (snapshot != null && snapshot.exists()) {
//                        Map<String, Object> savedPlaceData = snapshot.getData();
//                        if (savedPlaceData != null) {
//                            itemList.clear();
//                            for (String field : savedPlaceData.keySet()) {
//                                Map<String, Object> dataMap1 = (Map<String, Object>) savedPlaceData.get(field);
//                                if (dataMap1 != null) {
//                                    String itemName = (String) dataMap1.get("spot_name");
//                                    GeoPoint spotCoordinates = (GeoPoint) dataMap1.get("spot_coordinates");
//                                    String spotImageURL = (String) dataMap1.get("spot_image_url");
//                                    String spotNameReference = (String) dataMap1.get("spot_name_reference");
//
//                                    assert spotCoordinates != null;
//                                    AllLocationsViewModel item = new AllLocationsViewModel(itemName, String.valueOf(spotCoordinates.getLatitude()),
//                                            String.valueOf(spotCoordinates.getLongitude()), spotNameReference, spotImageURL);
//                                    itemList.add(item);
//                                }
//                            }
//                            // Update the GridView adapter
//                            allLocationsViewAdapter.notifyDataSetChanged();
//                        }
//                    } else {
//                        Log.d(LOG_TAG, "Current data: null");
//                    }
//                });
//
//        allLocationsViewAdapter = new AllLocationsViewAdapter(itemList, requireContext(), this);
//        gridView.setAdapter(allLocationsViewAdapter);
//    }

//    public static void updatePlace(String pl, String path) {
//        sharedDataView.setPlace(pl);
//        sharedDataView.setDocPath(path);
//    }

    @Override
    public void onSearchQuerySubmitted(String query) {
        Log.i(LOG_TAG, "onSearchQuerySubmitted: " + query);
    }

    @Override
    public void onSearchQueryUpdated(String query) {
        filteredList.clear();
        for (CampusSpotsModel item : itemList) {
            if (item.getSpot_name().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        campusSpotsAdapter.filterList(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
