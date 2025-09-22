package com.example.aromaatlas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class OwnerHomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_home_page);

        Button btnDashboard = findViewById(R.id.btnDashboard);
        Button btnPinLocation = findViewById(R.id.btnPinLocation);
        Button btnMenu = findViewById(R.id.btnMenu);
        Button btnProfile = findViewById(R.id.btnProfile);

        btnDashboard.setOnClickListener(view -> {
            startActivity(new Intent(this, OwnerDashboardActivity.class));
        });

        btnPinLocation.setOnClickListener(view -> {
            startActivity(new Intent(this, OwnerPinLocationActivity.class));
        });

        btnMenu.setOnClickListener(view -> {
            startActivity(new Intent(this, OwnerMenuDashboardActivity.class));
        });

        btnProfile.setOnClickListener(view -> {
            startActivity(new Intent(this, OwnerProfileActivity.class));
        });
    }
}
