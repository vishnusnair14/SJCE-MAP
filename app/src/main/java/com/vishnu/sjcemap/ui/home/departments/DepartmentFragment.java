package com.vishnu.sjcemap.ui.home.departments;

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
import com.vishnu.sjcemap.databinding.FragmentDepartmentBinding;
import com.vishnu.sjcemap.miscellaneous.SharedDataView;

import java.util.ArrayList;
import java.util.List;

public class DepartmentFragment extends Fragment implements SearchQueryListener {
    FragmentDepartmentBinding binding;
    public static SharedDataView sharedDataView;
    List<AllDepartmentModel> deptList;
    AllDepartmentAdapter allDepartmentAdapter;
    private final String LOG_TAG = "DepartmentFragment";
    FirebaseFirestore db;
    List<AllDepartmentModel> filteredList;
    GridView gridView;
    CollectionReference ordersRef;

    public DepartmentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
//        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
        deptList = new ArrayList<>();
        filteredList = new ArrayList<>();

        ordersRef = FirebaseFirestore.getInstance()
                .collection("AllLocations").document("allDepartments")
                .collection("data");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = com.vishnu.sjcemap.databinding.FragmentDepartmentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        gridView = binding.allDepartmentGridView;
        allDepartmentAdapter = new AllDepartmentAdapter(deptList, requireContext(), this);
        gridView.setAdapter(allDepartmentAdapter);

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
                    deptList.clear();  // Clear the list before adding new items
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        if (doc.exists()) {
                            Log.d(LOG_TAG, "All departments data: " + doc.getData());
                            AllDepartmentModel order = doc.toObject(AllDepartmentModel.class);
                            deptList.add(order);
                        }
                    }
                    allDepartmentAdapter.notifyDataSetChanged();
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


//
//    @SuppressLint("NotifyDataSetChanged")
//    private void syncSavedPlacesRecycleView(@NonNull FragmentDepartmentBinding binding) {
//
//        GridView gridView = binding.allDepartmentRecycleView;
//
//        itemList.clear();
//
//        // Add snapshot listener to listen for real-time updates
//        db.collection("LocationData").document("DepartmentsLocationData").get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        DocumentSnapshot snapshot = task.getResult();
//                        if (snapshot != null && snapshot.exists()) {
//                            Map<String, Object> allDepartmentData = snapshot.getData();
//                            if (allDepartmentData != null) {
//                                itemList.clear();
//                                for (String field : allDepartmentData.keySet()) {
//                                    Map<String, Object> dataMap1 = (Map<String, Object>) allDepartmentData.get(field);
//                                    if (dataMap1 != null) {
//                                        String itemName = (String) dataMap1.get("spot_name");
//                                        GeoPoint spotCoordinates = (GeoPoint) dataMap1.get("spot_coordinates");
//                                        String spotImageURL = (String) dataMap1.get("spot_image_url");
//                                        String spotNameReference = (String) dataMap1.get("spot_name_reference");
//
//                                        assert spotCoordinates != null;
//                                        AllDepartmentsViewModel item = new AllDepartmentsViewModel(
//                                                itemName,
//                                                String.valueOf(spotCoordinates.getLatitude()),
//                                                String.valueOf(spotCoordinates.getLongitude()),
//                                                spotNameReference,
//                                                spotImageURL
//                                        );
//
//                                        itemList.add(item);
//                                    }
//                                }
//                                allDepartmentViewAdapter = new AllDepartmentViewAdapter(itemList, requireContext(), this);
//                                gridView.setAdapter(allDepartmentViewAdapter);
//                            }
//                        } else {
//                            Log.d("Firestore", "Current data: null");
//                        }
//                    } else {
//                        Log.w("Firestore", "Get failed.", task.getException());
//                    }
//                });
//
//    }

//
//    public static void updateDataToSharedView(String pl, String path) {
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
        for (AllDepartmentModel item : deptList) {
            if (item.getSpot_name().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        allDepartmentAdapter.filterList(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}