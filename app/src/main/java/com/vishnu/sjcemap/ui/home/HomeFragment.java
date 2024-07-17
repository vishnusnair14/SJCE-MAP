package com.vishnu.sjcemap.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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


public class HomeFragment extends Fragment {
    public static SharedDataView sharedDataView;
    private SharedPreferences authPreference;
    private static final String SCROLL_POSITION_KEY = "home_scroll_pos";
    ScrollView scrollView;
    private int savedScrollPosition = 0;
    TextView locNotEnabledTV;
    boolean isGpsEnabled = false;
    FirebaseFirestore db;
    private FragmentHomeBinding binding;
    private Bundle bundle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        bundle = new Bundle();

        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        authPreference = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        scrollView = binding.homeFragmentScrollView;
        locNotEnabledTV = binding.deviceLocNotEnabledInfoViewTextView;

        locNotEnabledTV.setOnClickListener(v -> {
            if (locNotEnabledTV.getVisibility() == View.VISIBLE) {
                showLocationSettings(requireContext());
            }
        });

        /* shortcut button click listeners */

        binding.viewAllTopLocationSCBButton2.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_nav_mainspots));

        binding.viewAllDepartmentSCBButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_departmentFragment));

        binding.sjceMainLibrarySCBButton.setOnClickListener(v -> navigateToMap("SJCELibrary"));

        binding.sjceISSeminarHallSCBButton.setOnClickListener(v -> navigateToMap("SJCEISSeminarHall"));

        binding.sjceReferenceSectionSCBButton.setOnClickListener(v -> navigateToMap("SJCEReferenceSection"));

        binding.sjceMainParkingSCBButton.setOnClickListener(v -> navigateToMap("SJCEMainParking"));

        binding.staffParking1SCBButton.setOnClickListener(v -> navigateToMap("StaffParking1"));

        binding.staffParking2SCBButton.setOnClickListener(v -> navigateToMap("StaffParking2"));

        binding.vehicleParking1SCBButton.setOnClickListener(v -> navigateToMap("VehicleParkingLot1"));

        binding.vehicleParking2SCBButton.setOnClickListener(v -> navigateToMap("VehicleParkingLot2"));

        binding.yampaCafeteriaSCBButton.setOnClickListener(v -> navigateToMap("Yampa"));

        binding.mylariCafeteriaSCBButton.setOnClickListener(v -> navigateToMap("AnnapoornaCanteen"));

        binding.exceedBoundaryBypassButton.setOnClickListener(v -> {
            authPreference.edit().putBoolean("isAlreadyScanned", false).apply();

            SoundNotify.playGeoFenceBoundaryExceedAlert();
            Toast.makeText(requireContext(), "Device exceeded geofence boundary, re-authentication required",
                    Toast.LENGTH_LONG).show();
        });

        return root;
    }

    private void navigateToMap(String docID) {
        bundle.clear();

        bundle.putString("doc_path", "otherCampusLocations");
        bundle.putString("doc_id", docID);
        bundle.putBoolean("load_from_db", true);

        NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_mapFragment, bundle);
    }

    private final BroadcastReceiver gpsStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GPSProviderService.ACTION_GPS_STATUS_CHANGED.equals(intent.getAction())) {
                boolean isGPSEnabled = intent.getBooleanExtra(GPSProviderService.EXTRA_IS_GPS_ENABLED, false);
                locNotEnabledTV.setVisibility(isGpsEnabled ? View.GONE : View.VISIBLE);

                if (isGPSEnabled && !isGpsEnabled) {
                    Toast.makeText(context, "@home: GPS Enabled", Toast.LENGTH_SHORT).show();

                    isGpsEnabled = true;
                } else if (!isGPSEnabled && isGpsEnabled) {
                    Toast.makeText(context, "@home: GPS Disabled", Toast.LENGTH_SHORT).show();

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

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onResume() {
        super.onResume();

        IntentFilter providerFilter = new IntentFilter(GPSProviderService.ACTION_GPS_STATUS_CHANGED);
        requireContext().registerReceiver(gpsStatusReceiver, providerFilter, Context.RECEIVER_NOT_EXPORTED);

        if (MainActivity.isLocationNotEnabled(requireContext())) {
            locNotEnabledTV.setVisibility(View.VISIBLE);
        } else {
            locNotEnabledTV.setVisibility(View.GONE);
        }
    }


    @Override
    public void onPause() {
        super.onPause();

        requireContext().unregisterReceiver(gpsStatusReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}