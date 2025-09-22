// File: OwnerMenuListActivity.java
package com.example.aromaatlas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class OwnerMenuListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirestoreRecyclerAdapter<MenuItemModel, MenuItemViewHolder> adapter;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private Spinner spnCafeFilter;
    private ImageButton backButton;
    private final List<String> cafeIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_menu_list);


        recyclerView = findViewById(R.id.recyclerMenu);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        spnCafeFilter = findViewById(R.id.spnCafeFilter);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore.collection("cafes")
                .whereEqualTo("ownerId", user.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    List<String> cafeNames = new ArrayList<>();
                    cafeIds.clear();
                    for (DocumentSnapshot doc : query) {
                        String name = doc.getString("name");
                        if (name != null) {
                            cafeNames.add(name);
                            cafeIds.add(doc.getId());
                        }
                    }

                    if (cafeNames.isEmpty()) {
                        Toast.makeText(this, "No cafes found for this owner", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, cafeNames
                    );
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spnCafeFilter.setAdapter(spinnerAdapter);

                    spnCafeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0 && position < cafeIds.size()) {
                                String selectedCafeId = cafeIds.get(position);
                                loadDrinksForCafe(user.getUid(), selectedCafeId);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load cafes: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadDrinksForCafe(String ownerId, String cafeId) {
        Query query = firestore.collection("drinks")
                .whereEqualTo("ownerId", ownerId)
                .whereEqualTo("cafeId", cafeId);

        FirestoreRecyclerOptions<MenuItemModel> options = new FirestoreRecyclerOptions.Builder<MenuItemModel>()
                .setQuery(query, MenuItemModel.class)
                .build();

        if (adapter != null) adapter.stopListening();

        adapter = new FirestoreRecyclerAdapter<MenuItemModel, MenuItemViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MenuItemViewHolder holder, int position, @NonNull MenuItemModel model) {
                holder.bind(model);

                holder.btnDelete.setOnClickListener(v -> {
                    getSnapshots().getSnapshot(position).getReference().delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(OwnerMenuListActivity.this, "Deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(OwnerMenuListActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });

                holder.btnEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(OwnerMenuListActivity.this, OwnerEditMenuActivity.class);
                    intent.putExtra("menuItemId", getSnapshots().getSnapshot(position).getId());
                    startActivity(intent);
                });
            }

            @NonNull
            @Override
            public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_card, parent, false);
                return new MenuItemViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}
