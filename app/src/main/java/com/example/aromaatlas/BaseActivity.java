package com.example.aromaatlas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    public static final int NAV_OWNER = 1;
    public static final int NAV_CUSTOMER = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base); // must include content_frame + bottom_nav
    }

    protected void setActivityLayout(int layoutResId) {
        FrameLayout frame = findViewById(R.id.content_frame);
        getLayoutInflater().inflate(layoutResId, frame);
    }

    protected void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) return;

        // 🔄 Load role from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String role = prefs.getString("user_role", "customer");

        // 📦 Inflate the correct menu
        bottomNav.getMenu().clear();
        if ("owner".equalsIgnoreCase(role)) {
            bottomNav.inflateMenu(R.menu.menu_owner);
        } else {
            bottomNav.inflateMenu(R.menu.menu_customer);
        }

        // ✅ Temporarily disable listener
        bottomNav.setOnItemSelectedListener(null);
        highlightCurrentMenuItem(bottomNav);

        // ✅ Re-attach listener after setting selectedItemId
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if ("owner".equalsIgnoreCase(role)) {
                handleOwnerNavigation(itemId);
            } else {
                handleCustomerNavigation(itemId);
            }
            return true;
        });

        // ✅ Highlight current item
        highlightCurrentMenuItem(bottomNav);
    }

    private void highlightCurrentMenuItem(BottomNavigationView bottomNav) {
        Class<?> current = this.getClass();

        if (current == OwnerDashboardActivity.class) {
            bottomNav.setSelectedItemId(R.id.nav_cafe_profile);
        } else if (current == OwnerPinLocationActivity.class) {
            bottomNav.setSelectedItemId(R.id.nav_location);
        } else if (current == OwnerMenuDashboardActivity.class) {
            bottomNav.setSelectedItemId(R.id.nav_list_menu);
        } else if (current == OwnerProfileActivity.class) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        } else if (current == OwnerHomePageActivity.class) {
            bottomNav.setSelectedItemId(R.id.nav_home_page);
        } else if (current == MainActivity.class) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else if (current == NearbyMapActivity.class) {
            bottomNav.setSelectedItemId(R.id.nav_nearby);
        } else if (current == QuizActivity.class) {
            bottomNav.setSelectedItemId(R.id.nav_beans);
        } else if (current == ProfileActivity.class) {
            bottomNav.setSelectedItemId(R.id.nav_profile2);
        }
    }

    private void navigateTo(Class<?> activityClass) {
        if (!this.getClass().equals(activityClass)) {
            Log.d("BaseActivity", "Navigating to: " + activityClass.getSimpleName());
            Intent intent = new Intent(this, activityClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private void handleOwnerNavigation(int itemId) {
        if (itemId == R.id.nav_cafe_profile) {
            navigateTo(OwnerDashboardActivity.class);
        } else if (itemId == R.id.nav_location) {
            navigateTo(OwnerPinLocationActivity.class);
        } else if (itemId == R.id.nav_home_page) {
            navigateTo(OwnerHomePageActivity.class);
        } else if (itemId == R.id.nav_list_menu) {
            navigateTo(OwnerMenuDashboardActivity.class);
        } else if (itemId == R.id.nav_profile) {
            navigateTo(OwnerProfileActivity.class);
        }
    }

    private void handleCustomerNavigation(int itemId) {
        if (itemId == R.id.nav_home) {
            navigateTo(MainActivity.class);
        } else if (itemId == R.id.nav_nearby) {
            navigateTo(NearbyMapActivity.class);
        } else if (itemId == R.id.nav_beans) {
            navigateTo(QuizActivity.class);
        } else if (itemId == R.id.nav_profile2) {
            navigateTo(ProfileActivity.class);
        }
    }
}
