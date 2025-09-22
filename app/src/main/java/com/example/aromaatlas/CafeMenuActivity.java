package com.example.aromaatlas;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CafeMenuActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CafeMenuAdapter adapter;
    private List<MenuItemModel> menuList = new ArrayList<>();
    private FirebaseFirestore db;
    private ImageButton backButton;
    private ListenerRegistration menuListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cafe_menu);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerMenu);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CafeMenuAdapter(menuList);
        recyclerView.setAdapter(adapter);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        String ownerId = getIntent().getStringExtra("ownerId");
        String cafeId = getIntent().getStringExtra("cafeId");

        if (ownerId == null || cafeId == null) {
            Toast.makeText(this, "Missing ownerId or cafeId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        menuListener = db.collection("drinks")
                .whereEqualTo("ownerId", ownerId)
                .whereEqualTo("cafeId", cafeId)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to load drinks.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    menuList.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot) {
                            MenuItemModel item = doc.toObject(MenuItemModel.class);
                            if (item != null) {
                                item.setId(doc.getId());
                                item.setFavorite(FavoriteManager.isDrinkFavorite(this, item.getId()));
                                menuList.add(item);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FavoriteManager.loadAllFromFirebase(this); // Loads both drink + cafe favs
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (menuListener != null) {
            menuListener.remove();
            menuListener = null;
        }
    }
}
