package com.example.aromaatlas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Set your splash layout

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                String role = prefs.getString("user_role", "");
                if ("owner".equals(role)) {
                    startActivity(new Intent(this, OwnerHomePageActivity.class));
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                }
            } else {
                startActivity(new Intent(this, LoginPage.class));
            }
            finish();
        }, 1500); // 1.5 second splash delay
    }
}
