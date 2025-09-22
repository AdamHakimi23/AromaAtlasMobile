package com.example.aromaatlas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private RecyclerView cafeRecyclerView;
    private CafeAdapter cafeAdapter;
    private FirebaseFirestore db;
    private TextView txtLocation;
    private EditText edtSearch;
    private FusedLocationProviderClient fusedLocationClient;
    private Button allBtn, ArabicaBtn, LibericaBtn, RobustaBtn, selectedButton;
    private ImageView btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_main);
        setupBottomNavigation();

        // Initialize views
        txtLocation = findViewById(R.id.txtLocation);
        edtSearch = findViewById(R.id.edtSearch);
        allBtn = findViewById(R.id.allBtn);
        ArabicaBtn = findViewById(R.id.ArabicaBtn);
        LibericaBtn = findViewById(R.id.LibericaBtn);
        RobustaBtn = findViewById(R.id.RobustaBtn);
        btnProfile = findViewById(R.id.btnProfile);

        // Initialize RecyclerView
        cafeRecyclerView = findViewById(R.id.recyclerRecommendations);
        cafeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cafeAdapter = new CafeAdapter(new ArrayList<>());
        cafeRecyclerView.setAdapter(cafeAdapter);

        // Initialize Firebase and location
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Profile button
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Load location and cafes
        fetchLocation();
        loadCafes();

        // Button filters
        allBtn.setOnClickListener(view -> {
            filterBeans("All");
            updateButtonStyle(allBtn);
        });
        ArabicaBtn.setOnClickListener(view -> {
            filterBeans("Arabica");
            updateButtonStyle(ArabicaBtn);
        });
        LibericaBtn.setOnClickListener(view -> {
            filterBeans("Liberica");
            updateButtonStyle(LibericaBtn);
        });
        RobustaBtn.setOnClickListener(view -> {
            filterBeans("Robusta");
            updateButtonStyle(RobustaBtn);
        });

        // Search functionality
        edtSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();

                if (query.isEmpty()) {
                    loadCafes();
                    return;
                }

                List<Cafe> matchedCafes = new ArrayList<>();
                List<String> matchedCafeIds = new ArrayList<>();

                db.collection("cafes").get().addOnSuccessListener(cafeSnapshots -> {
                    for (DocumentSnapshot doc : cafeSnapshots) {
                        Cafe cafe = doc.toObject(Cafe.class);
                        if (cafe != null && cafe.getName().toLowerCase().contains(query)) {
                            matchedCafes.add(cafe);
                            matchedCafeIds.add(cafe.getCafeId());
                        }
                    }

                    db.collection("drinks").get().addOnSuccessListener(drinkSnapshots -> {
                        List<String> additionalCafeIds = new ArrayList<>();
                        for (DocumentSnapshot drinkDoc : drinkSnapshots) {
                            String drinkName = drinkDoc.getString("name");
                            String cafeId = drinkDoc.getString("cafeId");

                            if (drinkName != null && drinkName.toLowerCase().contains(query) && cafeId != null) {
                                if (!matchedCafeIds.contains(cafeId) && !additionalCafeIds.contains(cafeId)) {
                                    additionalCafeIds.add(cafeId);
                                }
                            }
                        }

                        if (additionalCafeIds.isEmpty()) {
                            cafeAdapter.updateData(matchedCafes);
                            return;
                        }

                        db.collection("cafes")
                                .whereIn("cafeId", additionalCafeIds)
                                .get()
                                .addOnSuccessListener(extraCafeDocs -> {
                                    for (DocumentSnapshot doc : extraCafeDocs) {
                                        Cafe cafe = doc.toObject(Cafe.class);
                                        if (cafe != null) {
                                            matchedCafes.add(cafe);
                                        }
                                    }
                                    cafeAdapter.updateData(matchedCafes);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Search", "Failed to fetch extra cafes", e);
                                    cafeAdapter.updateData(matchedCafes);
                                });

                    }).addOnFailureListener(e -> {
                        Log.e("Search", "Failed to query drinks", e);
                        cafeAdapter.updateData(matchedCafes);
                    });
                });
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                try {
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (!addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        String city = addr.getLocality() != null ? addr.getLocality() : "Unknown City";
                        String country = addr.getCountryName() != null ? addr.getCountryName() : "Unknown Country";
                        txtLocation.setText(city + ", " + country);
                    }
                } catch (Exception e) {
                    txtLocation.setText("Location Unavailable");
                    Log.e("GeoError", "Geocoder failed", e);
                }
            }
        });
    }

    private void filterBeans(String beanType) {
        if (beanType.equalsIgnoreCase("All")) {
            loadCafes();
            return;
        }

        db.collection("drinks")
                .whereEqualTo("bean", beanType)
                .get()
                .addOnSuccessListener(drinksSnapshots -> {
                    List<String> cafeIds = new ArrayList<>();
                    for (DocumentSnapshot doc : drinksSnapshots) {
                        String cafeId = doc.getString("cafeId");
                        if (cafeId != null && !cafeIds.contains(cafeId)) {
                            cafeIds.add(cafeId);
                        }
                    }

                    if (cafeIds.isEmpty()) {
                        cafeAdapter.updateData(new ArrayList<>());
                        return;
                    }

                    if (cafeIds.size() > 10) {
                        Toast.makeText(MainActivity.this, "Too many results, please refine your filter.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("cafes")
                            .whereIn("cafeId", cafeIds)
                            .get()
                            .addOnSuccessListener(cafeSnapshots -> {
                                List<Cafe> filteredCafes = new ArrayList<>();
                                for (DocumentSnapshot doc : cafeSnapshots) {
                                    Cafe cafe = doc.toObject(Cafe.class);
                                    if (cafe != null) {
                                        filteredCafes.add(cafe);
                                    }
                                }
                                cafeAdapter.updateData(filteredCafes);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("DEBUG", "Failed to load cafes: ", e);
                                Toast.makeText(MainActivity.this, "Failed to load cafes.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG", "Failed to load drinks: ", e);
                    Toast.makeText(MainActivity.this, "Failed to load drinks.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchCafeRatings() {
        db.collection("cafe_ratings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, List<Double>> ratingGroups = new HashMap<>();
                    Map<String, Integer> reviewCounts = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        String cafeId = doc.getString("cafeId");
                        Double rating = doc.getDouble("rating");

                        if (cafeId != null && rating != null) {
                            ratingGroups.computeIfAbsent(cafeId.trim(), k -> new ArrayList<>()).add(rating);
                        }
                    }

                    Map<String, Double> averageRatings = new HashMap<>();
                    for (Map.Entry<String, List<Double>> entry : ratingGroups.entrySet()) {
                        List<Double> ratings = entry.getValue();
                        double avg = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                        averageRatings.put(entry.getKey(), avg);
                        reviewCounts.put(entry.getKey(), ratings.size());
                    }

                    cafeAdapter.setCafeRatingsAndCounts(averageRatings, reviewCounts);
                })
                .addOnFailureListener(e -> {
                    Log.e("CafeRatings", "Failed to fetch ratings", e);
                });
    }

    private void loadCafes() {
        db.collection("cafes")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    List<Cafe> allCafes = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshots) {
                        Cafe cafe = doc.toObject(Cafe.class);
                        if (cafe != null) {
                            allCafes.add(cafe);
                        }
                    }
                    cafeAdapter.updateData(allCafes);
                    fetchCafeRatings(); // ✅ Fetch ratings after loading
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG", "Failed to load cafes: ", e);
                    Toast.makeText(MainActivity.this, "Failed to load cafes.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateButtonStyle(Button selected) {
        allBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.category));
        ArabicaBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.category));
        LibericaBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.category));
        RobustaBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.category));

        allBtn.setTextColor(Color.BLACK);
        ArabicaBtn.setTextColor(Color.BLACK);
        LibericaBtn.setTextColor(Color.BLACK);
        RobustaBtn.setTextColor(Color.BLACK);

        selected.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.selected_category));
        selected.setTextColor(Color.WHITE);

        selectedButton = selected;
    }
}
