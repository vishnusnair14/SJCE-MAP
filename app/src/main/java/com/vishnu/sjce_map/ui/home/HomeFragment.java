package com.vishnu.sjce_map.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.sjce_map.R;
import com.vishnu.sjce_map.databinding.FragmentHomeBinding;
import com.vishnu.sjce_map.miscellaneous.SharedDataView;

import java.text.DecimalFormat;
import java.text.MessageFormat;

public class HomeFragment extends Fragment {
    private static final String LOG_TAG = "HomeFragment";
    public static SharedDataView sharedDataView;
    FirebaseFirestore db;
    private FragmentHomeBinding binding;
    DecimalFormat coordinateFormat = new DecimalFormat("0.0000000000");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
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

        /* shortcut button click listeners */
        binding.registrationSectionSCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("registration_dept_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.adminBlockSCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("administrative_dept_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.principalOfficeSCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("principal_office_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.viewAllTopLocationSCBButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_nav_mainspots));

        binding.cseDeptSCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("cse_dept_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.mcaDeptSCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("mca_dept_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.eceDeptSCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("ece_dept_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.viewAllDepartmentSCBButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_departmentFragment));

        binding.sjceMainParkingSCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("sjce_main_parking_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.staffParking1SCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("staff_parking_1_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.staffParking2SCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("staff_parking_2_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.vehicleParking1SCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("sjce_vehicle_parking_1_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.vehicleParking2SCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("sjce_vehicle_parking_2_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.yampaCafeteriaSCBButton.setOnClickListener(v -> {

        });

        binding.mylariCafeteriaSCBButton.setOnClickListener(v -> {

        });


        return root;
    }

    private void updateShortcutBtnData(String pl) {
        sharedDataView.setPlace(pl);
        sharedDataView.setDocPath("ShortcutPlaceData");
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