package com.vishnu.sjce_map.ui.authentication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vishnu.sjce_map.R;

public class AuthenticationActivity extends AppCompatActivity {
    private static final String LOG_TAG = "LoginFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        LoginFragment loginFragment = new LoginFragment(this, this);
        RegisterFragment registerFragment = new RegisterFragment(this, this);

        transaction.add(R.id.fragmentContainer, loginFragment).commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

            if (item.getItemId() == R.id.login_nav_menu) {
                if (!loginFragment.isAdded()) {
                    fragmentTransaction.add(R.id.fragmentContainer, loginFragment);
                }
                fragmentTransaction.show(loginFragment);
                fragmentTransaction.hide(registerFragment);
            } else if (item.getItemId() == R.id.register_nav_menu) {
                if (!registerFragment.isAdded()) {
                    fragmentTransaction.add(R.id.fragmentContainer, registerFragment);
                }
                fragmentTransaction.show(registerFragment);
                fragmentTransaction.hide(loginFragment);
            }

            fragmentTransaction.commit();
            return true;
        });
    }

    public void finishActivity() {
        finish();
    }

}
