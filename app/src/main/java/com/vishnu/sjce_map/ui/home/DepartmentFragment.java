package com.vishnu.sjce_map.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.vishnu.sjce_map.MainActivity;
import com.vishnu.sjce_map.R;
import com.vishnu.sjce_map.databinding.FragmentDepartmentBinding;
import com.vishnu.sjce_map.miscellaneous.GridSpacingItemDecoration;
import com.vishnu.sjce_map.miscellaneous.SearchQueryListener;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;
import com.vishnu.sjce_map.view.AllDepartmentViewAdapter;
import com.vishnu.sjce_map.view.AllDepartmentsViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DepartmentFragment extends Fragment implements SearchQueryListener {
    FragmentDepartmentBinding binding;
    public static SharedDataView sharedDataView;
    List<AllDepartmentsViewModel> itemList = new ArrayList<>();
    AllDepartmentViewAdapter allDepartmentViewAdapter;
    private final String LOG_TAG = "DepartmentFragment";
    FirebaseFirestore db;

    public DepartmentFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the search query listener
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSearchQueryListener(this);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = com.vishnu.sjce_map.databinding.FragmentDepartmentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        syncSavedPlacesRecycleView(binding);

        return root;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void syncSavedPlacesRecycleView(@NonNull FragmentDepartmentBinding binding) {
        RecyclerView recyclerView = binding.allDepartmentRecycleView;
        GridLayoutManager homeLayoutManager = new GridLayoutManager(requireContext(), 2);
        recyclerView.setLayoutManager(homeLayoutManager);

        // Apply item decoration to set equal padding between items
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing); // Set your desired spacing
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacing, true));

        itemList.clear();

        // Add snapshot listener to listen for real-time updates
        db.collection("LocationData").document("DepartmentsLocationData")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> allDepartmentData = snapshot.getData();
                        if (allDepartmentData != null) {
                            itemList.clear();
                            for (String field : allDepartmentData.keySet()) {
                                Map<String, Object> dataMap1 = (Map<String, Object>) allDepartmentData.get(field);
                                if (dataMap1 != null) {
                                    String itemName = (String) dataMap1.get("spot_name");
                                    GeoPoint spotCoordinates = (GeoPoint) dataMap1.get("spot_coordinates");
                                    String spotImageURL = (String) dataMap1.get("spot_image_url");
                                    String spotNameReference = (String) dataMap1.get("spot_name_reference");

                                    assert spotCoordinates != null;
                                    AllDepartmentsViewModel item = new AllDepartmentsViewModel(itemName, String.valueOf(spotCoordinates.getLatitude()),
                                            String.valueOf(spotCoordinates.getLongitude()), spotNameReference, spotImageURL);
                                    itemList.add(item);
                                }
                            }
                            // Update the RecyclerView adapter
                            allDepartmentViewAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.d("Firestore", "Current data: null");
                    }
                });

        allDepartmentViewAdapter = new AllDepartmentViewAdapter(itemList, requireContext(), this);
        recyclerView.setAdapter(allDepartmentViewAdapter);
    }

    public static void updateDataToSharedView(String pl, String path) {
        sharedDataView.setPlace(pl);
        sharedDataView.setDocPath(path);
    }

    @Override
    public void onSearchQuerySubmitted(String query) {
        Log.i(LOG_TAG, "onSearchQuerySubmitted");
    }

    @Override
    public void onSearchQueryUpdated(String query) {
        List<AllDepartmentsViewModel> filteredList = new ArrayList<>();
        for (AllDepartmentsViewModel item : itemList) {
            if (item.getSpot_name().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        allDepartmentViewAdapter.filterList(filteredList);
    }
}