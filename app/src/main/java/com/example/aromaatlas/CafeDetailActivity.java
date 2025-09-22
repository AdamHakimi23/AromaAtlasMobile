package com.example.aromaatlas;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CafeDetailActivity extends AppCompatActivity {

    private TextView tvName, tvDescription, tvWorkingHours, tvWorkingDay;
    private TextView tvRating, tvStatus, tvRatingStatus;
    private ImageView ivCafeImage;
    private Button btnViewOnMap, btnSeeMenu, btnBooking, btnRating;
    private double latitude, longitude;
    private String cafeName, description, workingDay, workingHours, imageUrl, ownerId, cafeId;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cafe_detail);

        tvName = findViewById(R.id.tvCafeName);
        tvDescription = findViewById(R.id.tvCafeDescription);
        tvWorkingDay = findViewById(R.id.tvWorkingDay);
        tvWorkingHours = findViewById(R.id.tvWorkingHours);
        ivCafeImage = findViewById(R.id.ivCafeImage);
        btnSeeMenu = findViewById(R.id.btnSeeMenu);
        btnBooking = findViewById(R.id.btnBooking);
        btnRating = findViewById(R.id.btnRating);
        btnViewOnMap = findViewById(R.id.btnViewOnMap);
        tvRating = findViewById(R.id.textViewCafeRating);
        tvStatus = findViewById(R.id.textViewStatus);
        tvRatingStatus = findViewById(R.id.textViewCafeRatingStatus);
        backButton = findViewById(R.id.backButton);

        Intent intent = getIntent();
        cafeName = intent.getStringExtra("cafeName");
        description = intent.getStringExtra("description");
        workingDay = intent.getStringExtra("workingDay");
        workingHours = intent.getStringExtra("workingHours");
        imageUrl = intent.getStringExtra("imageUrl");
        ownerId = intent.getStringExtra("ownerId");
        cafeId = intent.getStringExtra("cafeId");
        latitude = intent.getDoubleExtra("latitude", 0);
        longitude = intent.getDoubleExtra("longitude", 0);

        backButton.setOnClickListener(v -> finish());

        if (ownerId == null || ownerId.trim().isEmpty()) {
            Toast.makeText(this, "Missing cafe owner ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvName.setText(cafeName != null ? cafeName : "Cafe");
        tvDescription.setText(description != null ? description : "-");
        tvWorkingHours.setText("Working Hours: " + (workingHours != null ? workingHours : "-"));
        tvWorkingDay.setText("Working Days: " + formatWorkingDays(workingDay));

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .into(ivCafeImage);

        btnSeeMenu.setOnClickListener(v -> {
            Intent menuIntent = new Intent(this, CafeMenuActivity.class);
            menuIntent.putExtra("ownerId", ownerId);
            menuIntent.putExtra("cafeId", cafeId);
            startActivity(menuIntent);
        });

        btnBooking.setOnClickListener(v -> {
            Intent bookingIntent = new Intent(this, BookingActivity.class);
            bookingIntent.putExtra("ownerId", ownerId);
            bookingIntent.putExtra("cafeId", cafeId);
            startActivity(bookingIntent);
        });

        btnViewOnMap.setOnClickListener(v -> {
            String uri = "geo:" + latitude + "," + longitude + "?q=" + Uri.encode(latitude + "," + longitude + " (" + cafeName + ")");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Google Maps not found", Toast.LENGTH_SHORT).show();
            }
        });

        btnRating.setOnClickListener(v -> showRatingDialog());

        updateCafeStatus();
        fetchRatingAndStatus();
    }

    private void fetchRatingAndStatus() {
        FirebaseFirestore.getInstance().collection("cafe_ratings")
                .whereEqualTo("cafeId", cafeId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    float totalRating = 0;
                    int count = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Number rating = (Number) doc.get("rating");
                        if (rating != null) {
                            totalRating += rating.floatValue();
                            count++;
                        }
                    }

                    float average = count > 0 ? totalRating / count : 0;
                    tvRating.setText(String.format("Rating: %.1f", average));
                    tvRatingStatus.setText(count > 0 ? "Rated by users" : "No ratings yet");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Rating fetch failed", e);
                    tvRating.setText("Rating: -");
                    tvRatingStatus.setText("Rating unavailable");
                });
    }

    private void showRatingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        RatingBar dialogRatingBar = dialogView.findViewById(R.id.dialogRatingBar);
        Button submitRatingBtn = dialogView.findViewById(R.id.btnSubmitRating);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        submitRatingBtn.setOnClickListener(view -> {
            float ratingValue = dialogRatingBar.getRating();
            String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : "anonymous";

            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("userId", userId);
            ratingData.put("cafeId", cafeId);
            ratingData.put("rating", ratingValue);
            ratingData.put("timestamp", Timestamp.now());

            FirebaseFirestore.getInstance().collection("cafe_ratings")
                    .add(ratingData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Thanks for rating!", Toast.LENGTH_SHORT).show();
                        fetchRatingAndStatus();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("RatingError", "Rating failed", e);
                        Toast.makeText(this, "Failed to submit rating.", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void updateCafeStatus() {
        if (workingDay == null || workingHours == null) {
            tvStatus.setText("Status: Unknown");
            tvStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            return;
        }

        List<String> openDays = Arrays.stream(workingDay.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        String currentDay = LocalDate.now().getDayOfWeek().toString(); // MONDAY
        currentDay = currentDay.substring(0,1).toUpperCase() + currentDay.substring(1).toLowerCase(); // Monday

        if (!openDays.contains(currentDay)) {
            tvStatus.setText("Closed Today");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            return;
        }

        try {
            String[] parts = workingHours.split("-");
            if (parts.length != 2) {
                tvStatus.setText("Status: Unknown");
                tvStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                return;
            }

            LocalTime openTime = LocalTime.parse(parts[0].trim(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime closeTime = LocalTime.parse(parts[1].trim(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime now = LocalTime.now();

            if (!now.isBefore(openTime) && now.isBefore(closeTime)) {
                tvStatus.setText("Open Now");
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvStatus.setText("Closed Now");
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        } catch (DateTimeParseException e) {
            tvStatus.setText("Status: Unknown");
            tvStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }


        try {
            String[] parts = workingHours.split("-");
            if (parts.length != 2) {
                tvStatus.setText("Status: Unknown");
                return;
            }

            LocalTime openTime = LocalTime.parse(parts[0].trim(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime closeTime = LocalTime.parse(parts[1].trim(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime now = LocalTime.now();

            if (!now.isBefore(openTime) && now.isBefore(closeTime)) {
                tvStatus.setText("Open Now");
            } else {
                tvStatus.setText("Closed Now");
            }
        } catch (DateTimeParseException e) {
            tvStatus.setText("Status: Unknown");
        }
    }

    private String formatWorkingDays(String raw) {
        if (raw == null || raw.isEmpty()) return "-";

        List<String> DAYS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

        List<String> inputDays = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(DAYS::contains)
                .sorted(Comparator.comparingInt(DAYS::indexOf))
                .collect(Collectors.toList());

        List<String> result = new ArrayList<>();
        int i = 0;

        while (i < inputDays.size()) {
            int start = i;
            while (i + 1 < inputDays.size()
                    && DAYS.indexOf(inputDays.get(i + 1)) == DAYS.indexOf(inputDays.get(i)) + 1) {
                i++;
            }

            if (start == i) {
                result.add(inputDays.get(i));
            } else {
                result.add(inputDays.get(start) + " - " + inputDays.get(i));
            }
            i++;
        }

        return String.join(", ", result);
    }
}
