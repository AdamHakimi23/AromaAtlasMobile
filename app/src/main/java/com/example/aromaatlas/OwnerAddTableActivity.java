package com.example.aromaatlas;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerAddTableActivity extends AppCompatActivity{

    private EditText edtTableNumber, edtCapacity;
    private Button btnSaveTable;
    private Spinner spnCafe;
    private ImageButton backButton;

    private FirebaseFirestore firestore;
    private FirebaseUser user;

    private final List<String> cafeIds = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_add_table);

        edtTableNumber = findViewById(R.id.edtTableNumber);
        edtCapacity = findViewById(R.id.edtCapacity);
        btnSaveTable = findViewById(R.id.btnSaveTable);
        spnCafe = findViewById(R.id.spnCafe);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        firestore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load cafes owned by current user
        firestore.collection("cafes")
                .whereEqualTo("ownerId", user.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    List<String> cafeNames = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        cafeNames.add(doc.getString("name"));
                        cafeIds.add(doc.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, cafeNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spnCafe.setAdapter(adapter);
                });

        btnSaveTable.setOnClickListener(v -> {
            int selectedCafeIndex = spnCafe.getSelectedItemPosition();
            if (selectedCafeIndex < 0 || selectedCafeIndex >= cafeIds.size()) {
                Toast.makeText(this, "Please select a café", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedCafeId = cafeIds.get(selectedCafeIndex);
            String tableNumber = edtTableNumber.getText().toString().trim();
            String capacityStr = edtCapacity.getText().toString().trim();

            if (TextUtils.isEmpty(tableNumber) || TextUtils.isEmpty(capacityStr)) {
                Toast.makeText(this, "Please enter table number and capacity", Toast.LENGTH_SHORT).show();
                return;
            }

            int capacity;
            try {
                capacity = Integer.parseInt(capacityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid capacity", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> table = new HashMap<>();
            table.put("tableNo", tableNumber);
            table.put("capacity", capacity);
            table.put("status", "available");
            table.put("cafeId", selectedCafeId);
            table.put("ownerId", user.getUid());

            firestore.collection("tables")
                    .add(table)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Table added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
