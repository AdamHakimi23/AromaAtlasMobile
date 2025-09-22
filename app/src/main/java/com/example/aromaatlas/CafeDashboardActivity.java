package com.example.aromaatlas;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CafeDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LineChart lineChart;
    private TextView textAverageRating;
    private Spinner spinnerCafeSelector;
    private ImageButton backButton;
    private List<Cafe> cafeList = new ArrayList<>();
    private ArrayAdapter<Cafe> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cafe_dashboard);

        db = FirebaseFirestore.getInstance();
        lineChart = findViewById(R.id.lineChartRatings);
        textAverageRating = findViewById(R.id.textAverageRating);
        spinnerCafeSelector = findViewById(R.id.spinnerCafeSelector);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        setupSpinnerAdapter();
        loadCafesFromFirestore();
    }

    private void setupSpinnerAdapter() {
        spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cafeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCafeSelector.setAdapter(spinnerAdapter);

        spinnerCafeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Cafe selectedCafe = cafeList.get(position);
                fetchRatings(selectedCafe.getCafeId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadCafesFromFirestore() {
        String currentOwnerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("cafes")
                .whereEqualTo("ownerId", currentOwnerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    cafeList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Cafe cafe = doc.toObject(Cafe.class);
                        if (cafe != null && cafe.getCafeId() != null) {
                            cafeList.add(cafe);
                        }
                    }

                    spinnerAdapter.notifyDataSetChanged();

                    if (!cafeList.isEmpty()) {
                        fetchRatings(cafeList.get(0).getCafeId());
                    } else {
                        textAverageRating.setText("No cafes found for your account.");
                    }
                })
                .addOnFailureListener(e -> {
                    textAverageRating.setText("Failed to load cafes.");
                    e.printStackTrace();
                });
    }


    private void fetchRatings(String cafeId) {
        db.collection("cafe_ratings")
                .whereEqualTo("cafeId", cafeId)
                // .orderBy("timestamp") // 🔥 REMOVE THIS to test
                .get()
                .addOnSuccessListener(this::onRatingsLoaded)
                .addOnFailureListener(e -> {
                    textAverageRating.setText("Failed to load ratings.");
                    e.printStackTrace();
                });
    }


    private void onRatingsLoaded(QuerySnapshot querySnapshot) {
        List<Entry> entries = new ArrayList<>();
        double total = 0;
        int count = 0;

        for (DocumentSnapshot doc : querySnapshot) {
            Double rating = doc.getDouble("rating");
            Timestamp ts = doc.getTimestamp("timestamp");

            if (rating != null && ts != null) {
                entries.add(new Entry(count, rating.floatValue()));
                total += rating;
                count++;
            }
        }

        updateChart(entries);
        updateAverageText(total, count);
    }

    private void updateChart(List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Rating Over Time");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextSize(12f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    private void updateAverageText(double total, int count) {
        double avg = count > 0 ? total / count : 0.0;
        textAverageRating.setText(
                String.format("Average Rating: %.2f (%d reviews)", avg, count)
        );
    }
}
