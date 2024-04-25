package com.vishnu.sjce_map.ui.authentication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.sjce_map.MainActivity;
import com.vishnu.sjce_map.R;

import java.util.Objects;

public class HelpActivity extends AppCompatActivity {
    private final String LOG_TAG = "HelpActivity";
    FirebaseAuth mAuth;
    private FirebaseFirestore db;
    TextView statusTV;
    SharedPreferences preferences;
    EditText pswdET;
    Intent mainActivity;
    Button authBtn;
    EditText usernameET;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        mainActivity = new Intent(this, MainActivity.class);

        preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        statusTV = findViewById(R.id.statusViewHelpActivity_textView);
        usernameET = findViewById(R.id.emailFieldHelpActivity_editTextText);
        pswdET = findViewById(R.id.passwordFieldHelpActivity_editTextText);
        authBtn = findViewById(R.id.authenticateHelpActivity_button);

        authBtn.setOnClickListener(v -> {
            if (Objects.equals(preferences.getString("username", ""), usernameET.getText().toString()) &&
                    Objects.equals(preferences.getString("password", ""), pswdET.getText().toString())) {
                loginUser(usernameET.getText().toString(), pswdET.getText().toString());
            } else {
                Toast.makeText(this, "Provided invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendEmailVerification(@NonNull FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(LOG_TAG, "Verification email has sent successfully.");
                Toast.makeText(this, "Verification email has sent successfully", Toast.LENGTH_SHORT).show();
                statusTV.setText(R.string.email_verification_link_sent);
                // You can show a message to the user to check their email for verification
            } else {
                Toast.makeText(this, "Failed to send email verification", Toast.LENGTH_SHORT).show();
                Log.e(LOG_TAG, "Failed to send email verification.", task.getException());
            }
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null && user.isEmailVerified()) {
                    // User is signed in and email is verified
                    // Proceed to your main activity or dashboard
                    startActivity(mainActivity);
                    finish();
                } else {
                    statusTV.setText(R.string.please_verify_your_email_id);
//                  preferences.edit().putBoolean("isRegisteredUser", false).apply();
                    assert user != null;
                    sendEmailVerification(user);
                    Toast.makeText(this, "Please verify your email id", Toast.LENGTH_SHORT).show();
                }
            } else {
                // If sign in fails, display a message to the user.
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                Log.w(LOG_TAG, "signInWithEmail:failure", task.getException());
            }
        });
    }

}