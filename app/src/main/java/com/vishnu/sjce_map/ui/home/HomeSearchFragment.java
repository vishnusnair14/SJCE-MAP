package com.vishnu.sjce_map.ui.home;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.vishnu.sjce_map.R;
import com.vishnu.sjce_map.databinding.FragmentHomeBinding;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class HomeSearchFragment extends Fragment {
    private static final String LOG_TAG = "HomeFragment";
    DecimalFormat decimalFormat = new DecimalFormat("0.000000000");

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    public void search(Context context, String query, ListView lv, String[] lc, TextView ctv, TextView ptv) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.search_single_textview);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            // Handle item click, e.g., add the selected item to Firestore
            String selectedPlace = adapter.getItem(position);

            assert selectedPlace != null;
            fetchDataFromFirestore(selectedPlace, ctv, ptv);

            Toast.makeText(context, selectedPlace, Toast.LENGTH_SHORT).show();
        });


        // Initialize your data
        List<String> originalData = Arrays.asList(lc);
        adapter.addAll(originalData);

        List<String> filteredData = new ArrayList<>();
        if (!TextUtils.isEmpty(query)) {
            for (String item : originalData) {
                if (item.toLowerCase().contains(query.toLowerCase())) {
                    filteredData.add(item);
                }
            }
        } else {
            filteredData.addAll(originalData);
        }
        adapter.clear();
        adapter.addAll(filteredData);
        adapter.notifyDataSetChanged();
    }

    private void fetchDataFromFirestore(String key, TextView ltv, TextView ptv) {
        DocumentReference documentRef = FirebaseFirestore.getInstance().collection("LocationData").document("SJCECampusLocations");

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
                            String placeName = (String) dataMap.get("place_name");
                            GeoPoint geoPoint = (GeoPoint) dataMap.get("coordinates");
                            ptv.setText(placeName);
                            assert geoPoint != null;
                            ltv.setText(MessageFormat.format("{0}°N {1}°E", decimalFormat.format(geoPoint.getLatitude()),
                                    decimalFormat.format(geoPoint.getLongitude())));

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
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}