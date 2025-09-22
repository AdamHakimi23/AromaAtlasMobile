// File: OwnerPinLocationActivity.java
package com.example.aromaatlas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerPinLocationActivity extends BaseActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private Button btnSaveLocation;
    private Spinner spinnerCafes;

    private FusedLocationProviderClient fusedLocationClient;

    private final List<String> cafeNames = new ArrayList<>();
    private final List<String> cafeDocIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_owner_pin_location);
        setupBottomNavigation();

        spinnerCafes = findViewById(R.id.spinnerCafes);
        btnSaveLocation = findViewById(R.id.btnSaveLocation);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        loadUserCafesIntoSpinner();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnSaveLocation.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                saveLocationToFirestore(selectedLatLng);
            } else {
                Toast.makeText(this, "Tap a location on the map first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserCafesIntoSpinner() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("cafes")
                .whereEqualTo("ownerId", user.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No cafés found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        cafeNames.add(doc.getString("name"));
                        cafeDocIds.add(doc.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cafeNames);
                    spinnerCafes.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load cafés", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Pinned Location"));
            selectedLatLng = latLng;
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                            } else {
                                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                            }
                        });

            } catch (SecurityException e) {
                Toast.makeText(this, "Location permission error", Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveLocationToFirestore(LatLng location) {
        int selectedIndex = spinnerCafes.getSelectedItemPosition();
        if (selectedIndex == -1 || selectedIndex >= cafeDocIds.size()) {
            Toast.makeText(this, "Please select a café", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedDocId = cafeDocIds.get(selectedIndex);

        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", location.latitude);
        updates.put("longitude", location.longitude);

        FirebaseFirestore.getInstance()
                .collection("cafes")
                .document(selectedDocId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Location updated!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, OwnerMenuDashboardActivity.class);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

