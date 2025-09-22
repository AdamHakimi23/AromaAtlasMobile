package com.example.aromaatlas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerTableListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirestoreRecyclerAdapter<TableModel, TableViewHolder> adapter;
    private FirebaseFirestore firestore;
    private Spinner spinnerCafes;
    private final List<String> cafeIds = new ArrayList<>();
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_table_list);


        recyclerView = findViewById(R.id.recyclerTables);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        spinnerCafes = findViewById(R.id.spinnerCafes);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
                    spinnerCafes.setAdapter(adapter);

                    spinnerCafes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String selectedCafeId = cafeIds.get(position);
                            loadTablesForCafe(selectedCafeId);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
    }

    private void loadTablesForCafe(String cafeId) {
        Query query = firestore.collection("tables")
                .whereEqualTo("cafeId", cafeId)
                .orderBy("tableNo"); // <-- sort alphabetically by table number


        FirestoreRecyclerOptions<TableModel> options = new FirestoreRecyclerOptions.Builder<TableModel>()
                .setQuery(query, TableModel.class)
                .build();

        if (adapter != null) adapter.stopListening();

        adapter = new FirestoreRecyclerAdapter<TableModel, TableViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull TableViewHolder holder, int position, @NonNull TableModel model) {
                holder.bind(model);

                holder.btnDelete.setOnClickListener(v -> {
                    getSnapshots().getSnapshot(position).getReference().delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(OwnerTableListActivity.this, "Deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(OwnerTableListActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });

                holder.btnEdit.setOnClickListener(v -> {
                    String docId = getSnapshots().getSnapshot(position).getId();
                    showEditDialog(docId, model);
                });
            }

            @NonNull
            @Override
            public TableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_table_card, parent, false);
                return new TableViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void showEditDialog(String docId, TableModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Table");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_table, null);
        EditText edtTableNo = dialogView.findViewById(R.id.edtDialogTableNo);
        EditText edtCapacity = dialogView.findViewById(R.id.edtDialogCapacity);
        Spinner spnStatus = dialogView.findViewById(R.id.spnDialogStatus);

        edtTableNo.setText(model.getTableNo());
        edtCapacity.setText(String.valueOf(model.getCapacity()));

        builder.setView(dialogView);

        // Set status options
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"available", "reserved"});
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnStatus.setAdapter(statusAdapter);

        // Pre-select current value
        int statusIndex = model.getStatus().equalsIgnoreCase("reserved") ? 1 : 0;
        spnStatus.setSelection(statusIndex);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newTableNo = edtTableNo.getText().toString().trim();
            String newCapacityStr = edtCapacity.getText().toString().trim();

            if (newTableNo.isEmpty() || newCapacityStr.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            int newCapacity = Integer.parseInt(newCapacityStr);
            String newStatus = spnStatus.getSelectedItem().toString();

            Map<String, Object> updated = new HashMap<>();
            updated.put("tableNo", newTableNo);
            updated.put("capacity", newCapacity);
            updated.put("status", newStatus);

            firestore.collection("tables").document(docId)
                    .update(updated)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Table updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
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
