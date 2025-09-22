package com.example.aromaatlas;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class BookingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String cafeId, ownerId;
    private Button btnBookTable, btnPickupOrder, btnPickDateTime, btnViewOrderStatus, btnViewBook;
    private TextView tvSelectedDateTime;
    private Calendar selectedDateTime;
    private boolean isDateTimeSelected = false;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();
        cafeId = getIntent().getStringExtra("cafeId");
        ownerId = getIntent().getStringExtra("ownerId");

        // Init buttons
        btnBookTable = findViewById(R.id.btnBookTable);
        btnPickupOrder = findViewById(R.id.btnPickupOrder);
        btnPickDateTime = findViewById(R.id.btnPickDateTime);
        btnViewOrderStatus = findViewById(R.id.btnViewOrderStatus);
        btnViewBook = findViewById(R.id.btnViewBook);
        tvSelectedDateTime = findViewById(R.id.tvSelectedDateTime);
        backButton = findViewById(R.id.backButton);
        selectedDateTime = Calendar.getInstance();

        // Click: Go Back
        backButton.setOnClickListener(v -> finish());

        // Click: Select DateTime
        btnPickDateTime.setOnClickListener(v -> showDateTimePicker());

        // Click: View Order Status
        btnViewOrderStatus.setOnClickListener(v -> showOrderStatusDialog());

        // Click: View Booked Tables
        btnViewBook.setOnClickListener(v -> showBookedTablesPopup());

        // Click: Book Table
        btnBookTable.setOnClickListener(v -> {
            if (!isDateTimeSelected) {
                Toast.makeText(this, "Please select a booking date & time", Toast.LENGTH_SHORT).show();
                return;
            }
            showAvailableTables();
        });

        // Click: Pickup Order
        btnPickupOrder.setOnClickListener(v -> {
            if (!isDateTimeSelected) {
                Toast.makeText(this, "Please select a pickup date & time", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, CustomerPickupOrderActivity.class);
            intent.putExtra("cafeId", cafeId);
            intent.putExtra("ownerId", ownerId);
            intent.putExtra("pickupDateTimeMillis", selectedDateTime.getTimeInMillis());
            startActivity(intent);
        });
    }

    private void showOrderStatusDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || cafeId == null) return;

        db.collection("orders")
                .whereEqualTo("customerId", user.getUid())
                .whereEqualTo("cafeId", cafeId) // 🔍 Important line
                .whereGreaterThan("pickupDateTime", new Timestamp(new Date(0)))
                .orderBy("pickupDateTime")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot orderDoc = snapshot.getDocuments().get(0);
                        String status = orderDoc.getString("status");
                        Timestamp pickupTimestamp = orderDoc.getTimestamp("pickupDateTime");

                        StringBuilder summary = new StringBuilder();
                        List<Map<String, Object>> items = (List<Map<String, Object>>) orderDoc.get("items");

                        if (items != null) {
                            for (Map<String, Object> item : items) {
                                String name = (String) item.get("drinkName");
                                Long qty = (Long) item.get("quantity");
                                summary.append("- ").append(name).append(" x").append(qty).append("\n");
                            }
                        }

                        String pickupTime = pickupTimestamp != null
                                ? android.text.format.DateFormat.format("dd MMM yyyy, hh:mm a", pickupTimestamp.toDate()).toString()
                                : "N/A";

                        new AlertDialog.Builder(this)
                                .setTitle("Latest Order")
                                .setMessage("Status: " + status + "\nPickup: " + pickupTime + "\n\n" + summary)
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("No Orders Found")
                                .setMessage("You have not placed any orders yet.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load order status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private void showDateTimePicker() {
        Calendar now = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateTime.set(Calendar.YEAR, year);
            selectedDateTime.set(Calendar.MONTH, month);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            TimePickerDialog timePicker = new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);

                String formatted = String.format(Locale.getDefault(),
                        "%02d/%02d/%04d %02d:%02d",
                        dayOfMonth, month + 1, year, hourOfDay, minute);

                tvSelectedDateTime.setText("Selected: " + formatted);
                isDateTimeSelected = true;
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);

            timePicker.show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
    }

    private void showAvailableTables() {
        Timestamp now = Timestamp.now();

        db.collection("tables")
                .whereEqualTo("cafeId", cafeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> tables = queryDocumentSnapshots.getDocuments();
                    List<DocumentSnapshot> availableTables = new ArrayList<>();

                    for (DocumentSnapshot doc : tables) {
                        String status = doc.getString("status");
                        Timestamp endTime = doc.contains("endTime") ? doc.getTimestamp("endTime") : null;

                        boolean isExpired = "reserved".equals(status) && endTime != null && endTime.toDate().before(new Date());

                        if ("available".equals(status) || isExpired) {
                            if (isExpired) {
                                db.collection("tables").document(doc.getId())
                                        .update("status", "available", "endTime", null);
                            }
                            availableTables.add(doc);
                        }
                    }

                    if (availableTables.isEmpty()) {
                        Toast.makeText(this, "No available tables", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] tableLabels = new String[availableTables.size()];
                    for (int i = 0; i < availableTables.size(); i++) {
                        DocumentSnapshot doc = availableTables.get(i);
                        String tableNo = doc.getString("tableNo");
                        String tableName = doc.contains("tableName") ? doc.getString("tableName") : "Table " + tableNo;
                        Long capacity = doc.getLong("capacity");

                        tableLabels[i] = capacity != null
                                ? tableName + " (" + capacity + " seats)"
                                : tableName;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Select a Table")
                            .setItems(tableLabels, (dialog, which) -> {
                                DocumentSnapshot selectedTable = availableTables.get(which);
                                bookTable(selectedTable);
                            })
                            .show();
                });
    }

    private void showBookedTablesPopup() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("Bookings")
                .whereEqualTo("cafeId", cafeId)
                .whereEqualTo("userId", user.getUid()) // ✅ filter by user
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> bookedTables = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        String tableNo = doc.getString("tableNo");
                        Timestamp ts = doc.getTimestamp("bookingDateTime");
                        String time = ts != null
                                ? android.text.format.DateFormat.format("dd MMM, hh:mm a", ts.toDate()).toString()
                                : "N/A";

                        bookedTables.add("Table " + tableNo + " @ " + time);
                    }

                    if (bookedTables.isEmpty()) {
                        bookedTables.add("No bookings yet.");
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Booked Tables")
                            .setItems(bookedTables.toArray(new String[0]), null)
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    private void bookTable(DocumentSnapshot tableDoc) {
        String tableId = tableDoc.getId();
        String tableNo = tableDoc.getString("tableNo");
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Calendar endDateTime = (Calendar) selectedDateTime.clone();
        endDateTime.add(Calendar.HOUR_OF_DAY, 1);

        Timestamp startTimestamp = new Timestamp(selectedDateTime.getTime());
        Timestamp endTimestamp = new Timestamp(endDateTime.getTime());

        db.collection("tables").document(tableId)
                .update("status", "reserved", "endTime", endTimestamp)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> booking = new HashMap<>();
                    booking.put("userId", userId);
                    booking.put("tableNo", tableNo);
                    booking.put("cafeId", cafeId);
                    booking.put("ownerId", ownerId);
                    booking.put("bookingType", "table");
                    booking.put("timestamp", FieldValue.serverTimestamp());
                    booking.put("bookingDateTime", startTimestamp);
                    booking.put("endTime", endTimestamp);

                    db.collection("Bookings")
                            .add(booking)
                            .addOnSuccessListener(doc ->
                                    Toast.makeText(this, "Booking confirmed!", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Booking failed!", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to book table!", Toast.LENGTH_SHORT).show()
                );
    }
}
