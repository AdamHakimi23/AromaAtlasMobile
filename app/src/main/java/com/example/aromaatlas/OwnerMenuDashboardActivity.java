// File: OwnerMenuDashboardActivity.java
package com.example.aromaatlas;

import static com.example.aromaatlas.BaseActivity.NAV_OWNER;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class OwnerMenuDashboardActivity extends BaseActivity {

    private MaterialButton btnAddMenu, btnViewMenuList, btnAddTable, btnViewTable, btnViewDashboard,btnViewBookOrder;
    private String ownerId;
    private String cafeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setActivityLayout(R.layout.activity_owner_menu_dashboard);
        setupBottomNavigation();

        // ✅ Get intent extras properly inside onCreate
        ownerId = getIntent().getStringExtra("ownerId");
        cafeId = getIntent().getStringExtra("cafeId");

        Log.d("OwnerMenuDashboard", "✅ ownerId: " + ownerId + ", cafeId: " + cafeId);

        btnAddMenu = findViewById(R.id.btnAddMenu);
        btnViewMenuList = findViewById(R.id.btnViewMenuList);
        btnAddTable = findViewById(R.id.btnAddTable);
        btnViewTable = findViewById(R.id.btnViewTable);
        btnViewBookOrder = findViewById(R.id.btnViewBookOrder);
        btnViewDashboard = findViewById(R.id.btnViewDashboard);


        btnAddMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerAddMenuActivity.class);
            intent.putExtra("ownerId", ownerId);
            intent.putExtra("cafeId", cafeId);
            startActivity(intent);
        });

        btnViewMenuList.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerMenuListActivity.class);
            intent.putExtra("ownerId", ownerId);
            intent.putExtra("cafeId", cafeId);
            startActivity(intent);
        });

        btnAddTable.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerAddTableActivity.class);
            intent.putExtra("ownerId", ownerId);
            intent.putExtra("cafeId", cafeId);
            startActivity(intent);
        });

        btnViewTable.setOnClickListener(v -> {
            Intent intent = new Intent(this, OwnerTableListActivity.class);
            intent.putExtra("ownerId", ownerId);
            intent.putExtra("cafeId", cafeId);
            startActivity(intent);
        });

        btnViewBookOrder.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, v);
            popupMenu.getMenu().add("Manage Book");
            popupMenu.getMenu().add("Manage Order");

            popupMenu.setOnMenuItemClickListener(item -> {
                Intent intent;
                switch (item.getTitle().toString()) {
                    case "Manage Book":
                        intent = new Intent(this, ManageBookActivity.class);
                        break;
                    case "Manage Order":
                        intent = new Intent(this, ManageOrderActivity.class);
                        break;
                    default:
                        return false;
                }

                intent.putExtra("ownerId", ownerId);
                intent.putExtra("cafeId", cafeId);
                startActivity(intent);
                return true;
            });

            popupMenu.show();
        });


        btnViewDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(this, CafeDashboardActivity.class);
            intent.putExtra("ownerId", ownerId);
            intent.putExtra("cafeId", cafeId);
            startActivity(intent);
        });
    }
}


