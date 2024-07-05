package com.vishnu.sjcemap.ui.about;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.sjcemap.R;

import java.util.Objects;

public class AboutFragment extends Fragment {
    DocumentReference dr;


    public AboutFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        dr = db.collection("DeveloperData").document("AppData");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView p = view.findViewById(R.id.privacyPolicyContent_textView);

        /* listener for: privacy_policy_link */
        dr.addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                if (snapshot.contains("privacy_policy_link")) {
                    if (!Objects.requireNonNull(snapshot.get("privacy_policy_link")).toString().isEmpty()) {
                        p.setText((String) snapshot.get("privacy_policy_link"));
                    } else {
                        p.setText("");
                    }
                }
            }
        });

        return view;
    }
}