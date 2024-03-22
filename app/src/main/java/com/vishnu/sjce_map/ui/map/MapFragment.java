package com.vishnu.sjce_map.ui.map;

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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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


public class MapFragment extends Fragment implements OnMapReadyCallback {
    SharedDataView sharedDataView;

    private static final String LOG_TAG = "MapFragment";
    DecimalFormat decimalFormat = new DecimalFormat("0.000000000");
    private GoogleMap mMap;
    private static final LatLng MY_LOCATION = new LatLng(12.316649940097205, 76.61397958678371);

    public MapFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
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

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void fetchDataFromFirestore(@NonNull String docPath, String key, TextView ltv, TextView ptv) {
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
//                    callback.onError("Error fetching document: " + task.getException().getMessage());
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Move the camera to a particular location and set the zoom level
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MY_LOCATION, 10));
    }
}