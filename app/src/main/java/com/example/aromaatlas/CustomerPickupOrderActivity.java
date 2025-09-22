package com.example.aromaatlas;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerPickupOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseFirestore firestore;
    private FirestoreRecyclerAdapter<MenuItemModel, MenuItemViewHolder> adapter;
    private String cafeId;
    private ImageButton backButton;
    private List<CartItem> cart = new ArrayList<>();
    private Timestamp pickupDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_pickup);

        Button btnCart = findViewById(R.id.btnCart);
        btnCart.setOnClickListener(v -> {
            Intent intent = new Intent(this, CartActivity.class);
            intent.putExtra("cart", new Gson().toJson(cart));
            intent.putExtra("cafeId", cafeId);
            if (pickupDateTime != null) {
                intent.putExtra("pickupDateTimeMillis", pickupDateTime.toDate().getTime()); // ✅ Correct way
            }
            startActivity(intent);
        });

        recyclerView = findViewById(R.id.recyclerPickupDrinks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        firestore = FirebaseFirestore.getInstance();
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        cafeId = getIntent().getStringExtra("cafeId");
        long pickupMillis = getIntent().getLongExtra("pickupDateTimeMillis", -1);
        if (pickupMillis != -1) {
            pickupDateTime = new Timestamp(new java.util.Date(pickupMillis));
        } else {
            pickupDateTime = null;
        }

        Query query = firestore.collection("drinks")
                .whereEqualTo("cafeId", cafeId);

        FirestoreRecyclerOptions<MenuItemModel> options = new FirestoreRecyclerOptions.Builder<MenuItemModel>()
                .setQuery(query, MenuItemModel.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<MenuItemModel, MenuItemViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MenuItemViewHolder holder, int position, @NonNull MenuItemModel model) {
                holder.txtName.setText(model.getName());
                holder.txtPrice.setText("RM " + model.getPrice());
                holder.txtDrinkType.setText(model.getDrink());

                Glide.with(holder.itemView.getContext())
                        .load(model.getImage())
                        .placeholder(R.drawable.placeholder)
                        .into(holder.imgMenu);

                holder.btnBook.setVisibility(View.VISIBLE);
                holder.btnBook.setText("Add to Cart");
                holder.btnBook.setOnClickListener(v -> {
                    showAddToCartDialog(model);
                });

                holder.btnEdit.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.GONE);
            }

            @NonNull
            @Override
            public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_menu_card, parent, false);
                return new MenuItemViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
    }

    private void showAddToCartDialog(MenuItemModel drink) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_order_drink, null);
        EditText edtQty = dialogView.findViewById(R.id.edtOrderQuantity);
        EditText edtNote = dialogView.findViewById(R.id.edtOrderNote);

        new AlertDialog.Builder(this)
                .setTitle("Add to Cart: " + drink.getName())
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String qtyStr = edtQty.getText().toString().trim();
                    String note = edtNote.getText().toString().trim();

                    if (qtyStr.isEmpty()) {
                        Toast.makeText(this, "Enter quantity", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity = Integer.parseInt(qtyStr);
                    cart.add(new CartItem(drink.getName(), drink.getDrink(), drink.getPrice(), quantity, note));
                    Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showOrderDialog(MenuItemModel drink) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_order_drink, null);
        EditText edtQty = dialogView.findViewById(R.id.edtOrderQuantity);
        EditText edtNote = dialogView.findViewById(R.id.edtOrderNote);

        new AlertDialog.Builder(this)
                .setTitle("Order " + drink.getName())
                .setView(dialogView)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String qtyStr = edtQty.getText().toString().trim();
                    String note = edtNote.getText().toString().trim();

                    if (qtyStr.isEmpty()) {
                        Toast.makeText(this, "Enter quantity", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity = Integer.parseInt(qtyStr);
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) return;

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
                                    order.put("drinkId", drink.getName());
                                    order.put("quantity", quantity);
                                    order.put("status", "pending");
                                    order.put("note", note);
                                    order.put("price", drink.getPrice());
                                    order.put("pickupDateTime", pickupDateTime); // ✅ Include pickup time here

                                    firestore.collection("orders")
                                            .add(order)
                                            .addOnSuccessListener(doc ->
                                                    Toast.makeText(this, "Order placed!", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
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
