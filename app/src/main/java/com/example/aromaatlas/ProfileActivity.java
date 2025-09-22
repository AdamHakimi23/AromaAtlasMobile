package com.example.aromaatlas;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aromaatlas.FavoriteDrinksAdapter;
import com.example.aromaatlas.BaseActivity;
import com.example.aromaatlas.Cafe;
import com.example.aromaatlas.CafeAdapter;
import com.example.aromaatlas.LoginPage;
import com.example.aromaatlas.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends BaseActivity {

    private TextView emailText, roleText, preferredBeanText;
    private Button logoutBtn;

    private RecyclerView recyclerFavorites;
    private CafeAdapter favoriteAdapter;

    private RecyclerView recyclerFavoriteDrinks;
    private FavoriteDrinksAdapter favoriteDrinksAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_profile);
        setupBottomNavigation();

        emailText = findViewById(R.id.emailText);
        roleText = findViewById(R.id.roleText);
        preferredBeanText = findViewById(R.id.preferredBeanTextView);
        logoutBtn = findViewById(R.id.logoutBtn);
        recyclerFavorites = findViewById(R.id.recyclerFavorites);
        recyclerFavoriteDrinks = findViewById(R.id.recyclerFavoriteDrinks);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        String userId = mAuth.getCurrentUser().getUid();

        // Load user profile info
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String email = documentSnapshot.getString("email");
                        String role = documentSnapshot.getString("role");
                        String preferredBean = documentSnapshot.getString("preferredBean");

                        emailText.setText("Email: " + email);
                        roleText.setText("Role: " + role);
                        preferredBeanText.setText("Preferred Bean: " + (preferredBean != null ? preferredBean : "Not set"));
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                });


        // Load favorite cafes
        db.collection("users").document(userId).collection("cafeFavorites")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> cafeIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        cafeIds.add(doc.getId());
                    }

                    if (!cafeIds.isEmpty()) {
                        db.collection("cafes")
                                .whereIn("cafeId", cafeIds)
                                .get()
                                .addOnSuccessListener(cafeSnapshot -> {
                                    List<Cafe> favoriteCafes = new ArrayList<>();
                                    for (DocumentSnapshot cafeDoc : cafeSnapshot) {
                                        Cafe cafe = cafeDoc.toObject(Cafe.class);
                                        if (cafe != null) {
                                            favoriteCafes.add(cafe);
                                        }
                                    }
                                    favoriteAdapter = new CafeAdapter(favoriteCafes);
                                    recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
                                    recyclerFavorites.setAdapter(favoriteAdapter);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ProfileActivity", "Failed to load cafes", e);
                                    Toast.makeText(this, "Failed to load favorite cafes", Toast.LENGTH_SHORT).show();
                                });
                    }
                });

// Load favorite drinks
        db.collection("users").document(userId).collection("drinkFavorites")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> drinkIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        drinkIds.add(doc.getId());
                    }

                    if (!drinkIds.isEmpty()) {
                        db.collection("drinks")
                                .whereIn(FieldPath.documentId(), drinkIds)
                                .get()
                                .addOnSuccessListener(drinkSnapshot -> {
                                    List<MenuItemModel> favoriteDrinks = new ArrayList<>();
                                    for (DocumentSnapshot doc : drinkSnapshot) {
                                        MenuItemModel drink = doc.toObject(MenuItemModel.class);
                                        if (drink != null) {
                                            drink.setId(doc.getId());
                                            favoriteDrinks.add(drink);
                                        }
                                    }

                                    favoriteDrinksAdapter = new FavoriteDrinksAdapter(favoriteDrinks);
                                    recyclerFavoriteDrinks.setLayoutManager(new LinearLayoutManager(this));
                                    recyclerFavoriteDrinks.setAdapter(favoriteDrinksAdapter);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ProfileActivity", "Failed to load drinks", e);
                                    Toast.makeText(this, "Failed to load favorite drinks", Toast.LENGTH_SHORT).show();
                                });
                    }
                });


        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(ProfileActivity.this, LoginPage.class));
            finish();
        });
    }
}
