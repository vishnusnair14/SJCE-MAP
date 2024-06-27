package com.vishnu.sjcemap.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.sjcemap.MainActivity;
import com.vishnu.sjcemap.R;
import com.vishnu.sjcemap.databinding.FragmentHomeBinding;
import com.vishnu.sjcemap.miscellaneous.SharedDataView;
import com.vishnu.sjcemap.miscellaneous.SoundNotify;
import com.vishnu.sjcemap.service.GPSProviderService;

import java.text.DecimalFormat;
import java.text.MessageFormat;

public class HomeFragment extends Fragment {
    private static final String LOG_TAG = "HomeFragment";
    public static SharedDataView sharedDataView;
    private SharedPreferences authPreference;
    private static final String SCROLL_POSITION_KEY = "scroll_position";
    ScrollView scrollView;
    private int savedScrollPosition = 0;
    boolean isGpsEnabled = false;
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

        authPreference = requireContext().getSharedPreferences("MyPrefs", MODE_PRIVATE);

        scrollView = binding.homeFragmentScrollView;


        binding.deviceLocNotEnabledInfoViewTextView.setOnClickListener(v -> {
            if (binding.deviceLocNotEnabledInfoViewTextView.getVisibility() == View.VISIBLE) {
                showLocationSettings(requireContext());
            }
        });

        /* shortcut button click listeners */
//        binding.registrationSectionSCBButton.setOnClickListener(v -> {
//            updateShortcutBtnData("registration_dept_scb");
//            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
//        });
//
//        binding.adminBlockSCBButton.setOnClickListener(v -> {
//            updateShortcutBtnData("administrative_dept_scb");
//            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
//        });
//
//        binding.principalOfficeSCBButton.setOnClickListener(v -> {
//            updateShortcutBtnData("principal_office_scb");
//            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
//        });
//

        binding.viewAllTopLocationSCBButton2.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_nav_mainspots));
//
//        binding.cseDeptSCBButton.setOnClickListener(v -> {
//            updateShortcutBtnData("cse_dept_scb");
//            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
//        });
//
//        binding.mcaDeptSCBButton.setOnClickListener(v -> {
//            updateShortcutBtnData("mca_dept_scb");
//            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
//        });
//
//        binding.eceDeptSCBButton.setOnClickListener(v -> {
//            updateShortcutBtnData("ece_dept_scb");
//            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
//        });

        binding.viewAllDepartmentSCBButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_departmentFragment));

        binding.sjceMainLibrarySCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("sjce_main_library_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.sjceISSeminarHallSCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("sjce_is_seminar_hall_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.sjceAuditorium2SCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("sjce_auditorium2_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

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
            updateShortcutBtnData("sjce_yampa_cafeteria_sbc");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.mylariCafeteriaSCBButton.setOnClickListener(v -> {
            updateShortcutBtnData("sjce_mylari_cafeteria_scb");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment);
        });

        binding.exceedBoundaryBypassButton.setOnClickListener(v -> {
            authPreference.edit().putBoolean("isAlreadyScanned", false).apply();

            SoundNotify.playGeoFenceBoundaryExceedAlert();
            Toast.makeText(requireContext(), "Device exceeded geofence boundary,\nre-authentication required",
                    Toast.LENGTH_LONG).show();
        });

        IntentFilter providerFilter = new IntentFilter(GPSProviderService.ACTION_GPS_STATUS_CHANGED);
        requireContext().registerReceiver(gpsStatusReceiver, providerFilter, Context.RECEIVER_NOT_EXPORTED);

        return root;
    }

    private final BroadcastReceiver gpsStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GPSProviderService.ACTION_GPS_STATUS_CHANGED.equals(intent.getAction())) {
                boolean isGPSEnabled = intent.getBooleanExtra(GPSProviderService.EXTRA_IS_GPS_ENABLED, false);
                binding.deviceLocNotEnabledInfoViewTextView.setVisibility(isGpsEnabled ? View.GONE : View.VISIBLE);

                if (isGPSEnabled && !isGpsEnabled) {
                    Toast.makeText(context, "GPS Enabled", Toast.LENGTH_SHORT).show();

                    isGpsEnabled = true;
                } else if (!isGPSEnabled && isGpsEnabled) {
                    Toast.makeText(context, "GPS Disabled", Toast.LENGTH_SHORT).show();

                    isGpsEnabled = false;
                }
            }
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            savedScrollPosition = savedInstanceState.getInt(SCROLL_POSITION_KEY, 0);
            scrollView.post(() -> scrollView.scrollTo(0, savedScrollPosition));
        }
    }

    private void updateShortcutBtnData(String pl) {
        sharedDataView.setPlace(pl);
        sharedDataView.setDocPath("ShortcutPlaceData");
    }

    private void showLocationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        savedScrollPosition = scrollView.getScrollY();
        outState.putInt(SCROLL_POSITION_KEY, savedScrollPosition);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (MainActivity.isLocationNotEnabled(requireContext())) {
            binding.deviceLocNotEnabledInfoViewTextView.setVisibility(View.VISIBLE);
        } else {
            binding.deviceLocNotEnabledInfoViewTextView.setVisibility(View.GONE);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}