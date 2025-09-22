package com.example.aromaatlas;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OwnerProfileActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private List<Cafe> cafeList;
    private CafeAdapter adapter;

    private TextView emailText, roleText;
    private Button logoutBtn, btnEditCafe;
    private String firstCafeId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_owner_profile);
        setupBottomNavigation();

        recyclerView = findViewById(R.id.recyclerCafes);
        cafeList = new ArrayList<>();
        adapter = new CafeAdapter(cafeList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        emailText = findViewById(R.id.emailText);
        roleText = findViewById(R.id.roleText);
        logoutBtn = findViewById(R.id.logoutBtn);
        btnEditCafe = findViewById(R.id.btnEditCafe);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.e("OwnerProfile", "User not logged in.");
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginPage.class));
            finish();
            return;
        }

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Load user profile
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String email = documentSnapshot.getString("email");
                        String role = documentSnapshot.getString("role");

                        emailText.setText("Email: " + email);
                        roleText.setText("Role: " + role);
                    } else {
                        Log.e("OwnerProfile", "User document not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("OwnerProfile", "Failed to fetch user profile", e);
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });

        // Load cafes owned by user
        db.collection("cafes")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d("OwnerProfile", "Fetched cafes count: " + snapshot.size());
                    if (snapshot.isEmpty()) {
                        Log.d("OwnerProfile", "No cafes found for ownerId: " + uid);
                        return;
                    }
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Log.d("OwnerProfile", "Cafe doc: " + doc.getData());
                        Cafe cafe = doc.toObject(Cafe.class);
                        if (cafe != null) {
                            cafeList.add(cafe);
                            if (firstCafeId == null) {
                                firstCafeId = doc.getId();
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("OwnerProfile", "Failed to load cafes", e);
                    Toast.makeText(this, "Failed to load cafés", Toast.LENGTH_SHORT).show();
                });

        // Logout
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(OwnerProfileActivity.this, LoginPage.class));
            finish();
        });

        // Edit Cafe
        btnEditCafe.setOnClickListener(v -> {
            if (firstCafeId != null) {
                Intent intent = new Intent(OwnerProfileActivity.this, OwnerEditCafeActivity.class);
                intent.putExtra("cafeId", firstCafeId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No café found to edit", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
