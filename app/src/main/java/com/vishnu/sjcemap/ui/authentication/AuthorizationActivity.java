package com.vishnu.sjcemap.ui.authentication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vishnu.sjcemap.R;

public class AuthorizationActivity extends AppCompatActivity {
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
