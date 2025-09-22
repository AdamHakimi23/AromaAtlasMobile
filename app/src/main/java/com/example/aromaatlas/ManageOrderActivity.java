package com.example.aromaatlas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManageOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter<OrderModel, OrderViewHolder> orderAdapter;
    private String cafeId, ownerId;
    private Spinner spnCafe;
    private TextView tvNoOrders;
    private ImageButton backButton;
    private Spinner spnStatusFilter;
    private String selectedStatus = "All";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_management);

        recyclerOrders = findViewById(R.id.recyclerOrders);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        tvNoOrders = findViewById(R.id.tvNoOrders);
        spnCafe = findViewById(R.id.spnCafe);
        spnStatusFilter = findViewById(R.id.spnStatusFilter); // ✅ moved here
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        spnStatusFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = parent.getItemAtPosition(position).toString();
                setupOrderAdapter(); // re-query Firestore
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            ownerId = user.getUid();
            loadCafeList();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show();
            finish();
        }
    }



    private void loadCafeList() {
        db.collection("cafes")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ArrayList<Cafe> cafes = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Cafe cafe = doc.toObject(Cafe.class);
                        if (cafe != null) cafes.add(cafe);
                    }

                    if (cafes.isEmpty()) {
                        Toast.makeText(this, "No cafes found for this owner.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ArrayAdapter<Cafe> cafeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cafes);
                    cafeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spnCafe.setAdapter(cafeAdapter);

                    spnCafe.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            Cafe selectedCafe = (Cafe) parent.getItemAtPosition(position);
                            cafeId = selectedCafe.getCafeId();
                            setupOrderAdapter(); // ✅ FIXED: must call this to load orders
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading cafes", Toast.LENGTH_SHORT).show());
    }

    private void setupOrderAdapter() {
        Query query = db.collection("orders").whereEqualTo("cafeId", cafeId);
        if (!selectedStatus.equals("All")) {
            query = query.whereEqualTo("status", selectedStatus);
        }


        FirestoreRecyclerOptions<OrderModel> options = new FirestoreRecyclerOptions.Builder<OrderModel>()
                .setQuery(query, OrderModel.class)
                .build();

        if (orderAdapter != null) orderAdapter.stopListening();

        orderAdapter = new FirestoreRecyclerAdapter<OrderModel, OrderViewHolder>(options) {

            @Override
            protected void onBindViewHolder(@NonNull OrderViewHolder holder, int position, @NonNull OrderModel model) {
                List<Map<String, Object>> items = model.getItems();
                StringBuilder drinkSummary = new StringBuilder();

                Timestamp pickupTime = model.getPickupDateTime();
                if (pickupTime != null) {
                    String formattedTime = android.text.format.DateFormat.format("dd MMM yyyy, hh:mm a", pickupTime.toDate()).toString();
                    holder.tvPickupTime.setText("Pickup: " + formattedTime);
                    holder.tvPickupTime.setVisibility(View.VISIBLE);
                } else {
                    holder.tvPickupTime.setVisibility(View.GONE);
                }


                if (items != null) {
                    for (Map<String, Object> item : items) {
                        String name = (String) item.get("drinkName");
                        Long qty = item.get("quantity") instanceof Long ? (Long) item.get("quantity") : 0L;
                        String note = (String) item.get("note");

                        drinkSummary.append("- ").append(name)
                                .append(" (x").append(qty).append(")")
                                .append(note != null && !note.isEmpty() ? " - " + note : "")
                                .append("\n");
                    }
                }

                holder.tvDrink.setText(drinkSummary.toString().trim());
                holder.tvQty.setVisibility(View.GONE); // ❌ Hide unused fields
                holder.tvNote.setVisibility(View.GONE); // ❌ Hide unused fields
                holder.tvStatus.setText("Status: " + model.getStatus());

                holder.btnMarkReady.setOnClickListener(v -> {
                    DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
                    snapshot.getReference().update("status", "ready")
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(ManageOrderActivity.this, "Marked Ready", Toast.LENGTH_SHORT).show()
                            );
                });
            }


            @NonNull
            @Override
            public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_card, parent, false);
                return new OrderViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                tvNoOrders.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        };

        recyclerOrders.setAdapter(orderAdapter);
        orderAdapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (orderAdapter != null) orderAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orderAdapter != null) orderAdapter.stopListening();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDrink, tvQty, tvNote, tvStatus;
        TextView tvPickupTime; // ✅ NEW
        Button btnMarkReady;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDrink = itemView.findViewById(R.id.tvDrink);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnMarkReady = itemView.findViewById(R.id.btnMarkReady);
            tvPickupTime = itemView.findViewById(R.id.tvPickupTime); // ✅ link view
        }
    }
}
