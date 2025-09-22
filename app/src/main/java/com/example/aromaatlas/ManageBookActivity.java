package com.example.aromaatlas;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ManageBookActivity extends AppCompatActivity {

    private RecyclerView recyclerBookings;
    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter<BookingModel, BookingViewHolder> bookingAdapter;
    private String cafeId, ownerId;
    private Spinner spnCafe, spnFilter;
    private TextView tvNoBookings;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_management);

        recyclerBookings = findViewById(R.id.recyclerBookings);
        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));
        tvNoBookings = findViewById(R.id.tvNoBookings);
        spnCafe = findViewById(R.id.spnCafe);
        spnFilter = findViewById(R.id.spnFilter); // 👈 new filter spinner
        db = FirebaseFirestore.getInstance();
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            ownerId = user.getUid();
            loadCafeList();
            setupFilterSpinner();
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
                            applyFilter();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading cafes", Toast.LENGTH_SHORT).show());
    }

    private void setupFilterSpinner() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"All", "Upcoming", "Past", "Today"});
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFilter.setAdapter(filterAdapter);

        spnFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilter() {
        if (cafeId == null || ownerId == null) return;

        String selectedFilter = (String) spnFilter.getSelectedItem();
        Query query = db.collection("Bookings")
                .whereEqualTo("cafeId", cafeId)
                .whereEqualTo("ownerId", ownerId);

        Timestamp now = new Timestamp(new Date());
        Calendar calendar = Calendar.getInstance();

        switch (selectedFilter) {
            case "Upcoming":
                query = query.whereGreaterThan("bookingDateTime", now);
                break;
            case "Past":
                query = query.whereLessThan("bookingDateTime", now);
                break;
            case "Today":
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                Timestamp startOfDay = new Timestamp(calendar.getTime());

                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                Timestamp endOfDay = new Timestamp(calendar.getTime());

                query = query.whereGreaterThanOrEqualTo("bookingDateTime", startOfDay)
                        .whereLessThanOrEqualTo("bookingDateTime", endOfDay);
                break;
            case "All":
            default:
                // No extra condition
                break;
        }

        setupBookingAdapter(query);
    }

    private void setupBookingAdapter(Query query) {
        FirestoreRecyclerOptions<BookingModel> options = new FirestoreRecyclerOptions.Builder<BookingModel>()
                .setQuery(query, BookingModel.class)
                .build();

        if (bookingAdapter != null) bookingAdapter.stopListening();

        bookingAdapter = new FirestoreRecyclerAdapter<BookingModel, BookingViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull BookingViewHolder holder, int position, @NonNull BookingModel model) {
                holder.tvTable.setText("Table: " + model.getTableNo());
                holder.tvCustomer.setText("User ID: " + model.getUserId());
                holder.tvType.setText("Type: " + model.getBookingType());
                holder.tvDateTime.setText("DateTime: " + formatDate(model.getBookingDateTime().toDate()));
            }

            @NonNull
            @Override
            public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_card, parent, false);
                return new BookingViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                tvNoBookings.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        };

        recyclerBookings.setAdapter(bookingAdapter);
        bookingAdapter.startListening();
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date);
    }

    @Override protected void onStart() {
        super.onStart();
        if (bookingAdapter != null) bookingAdapter.startListening();
    }

    @Override protected void onStop() {
        super.onStop();
        if (bookingAdapter != null) bookingAdapter.stopListening();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvTable, tvCustomer, tvType, tvDateTime;
        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTable = itemView.findViewById(R.id.tvTableNo);
            tvCustomer = itemView.findViewById(R.id.tvUserId);
            tvType = itemView.findViewById(R.id.tvBookingType);
            tvDateTime = itemView.findViewById(R.id.tvBookingDateTime);
        }
    }
}
