package com.example.aromaatlas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.reflect.TypeToken;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private List<CartItem> cartItems;
    private FirebaseFirestore firestore;
    private String cafeId;
    private Timestamp pickupDateTime;  // 🟡 Added timestamp variable
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.recyclerCartItems);
        firestore = FirebaseFirestore.getInstance();
        cafeId = getIntent().getStringExtra("cafeId");


        // 🟡 Get cart from intent
        String cartJson = getIntent().getStringExtra("cart");
        cartItems = new Gson().fromJson(cartJson, new TypeToken<List<CartItem>>() {
        }.getType());

        // 🟡 Get pickupDateTimeMillis and convert to Timestamp
        long pickupMillis = getIntent().getLongExtra("pickupDateTimeMillis", -1);
        if (pickupMillis != -1) {
            pickupDateTime = new Timestamp(new Date(pickupMillis));
        }

        // Set up RecyclerView
        adapter = new CartAdapter(cartItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Button to place the order
        findViewById(R.id.btnPlaceOrder).setOnClickListener(v -> placeOrder());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(CartActivity.this, CustomerPickupOrderActivity.class);
        startActivity(intent);
        finish(); // optional: prevents returning to current activity
    }


    private void placeOrder() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not signed in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch ownerId from the cafe
        firestore.collection("cafes")
                .whereEqualTo("cafeId", cafeId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        String ownerId = snapshot.getDocuments().get(0).getString("ownerId");

                        Map<String, Object> order = new HashMap<>();
                        order.put("customerId", user.getUid());
                        order.put("cafeId", cafeId);
                        order.put("ownerId", ownerId);
                        order.put("status", "pending");
                        order.put("pickupDateTime", pickupDateTime);

                        List<Map<String, Object>> orderItems = new java.util.ArrayList<>();
                        for (CartItem item : cartItems) {
                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("drinkName", item.getName());
                            itemMap.put("type", item.getDrinkType()); // ✅ Correct
                            itemMap.put("price", item.getPrice());
                            itemMap.put("quantity", item.getQuantity());
                            itemMap.put("note", item.getNote());
                            orderItems.add(itemMap);
                        }
                        order.put("items", orderItems);

                        firestore.collection("orders")
                                .add(order)
                                .addOnSuccessListener(doc -> {
                                    Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
    }
}
