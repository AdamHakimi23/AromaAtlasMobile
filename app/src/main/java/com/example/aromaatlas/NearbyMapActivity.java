// File: NearbyMapActivity.java
package com.example.aromaatlas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class NearbyMapActivity extends BaseActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private Location userLocation;
    private TextView txtCafeName, textViewWorkingDay, textViewWorkingHours, txtDistance;

    private String formatWorkingDays(String raw) {
        if (raw == null || raw.isEmpty()) return "-";

        List<String> DAYS = List.of(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        );

        List<String> inputDays = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(DAYS::contains)
                .sorted(Comparator.comparingInt(DAYS::indexOf))
                .collect(Collectors.toList());

        List<String> result = new ArrayList<>();
        int i = 0;

        while (i < inputDays.size()) {
            int start = i;
            while (i + 1 < inputDays.size() &&
                    DAYS.indexOf(inputDays.get(i + 1)) == DAYS.indexOf(inputDays.get(i)) + 1) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_nearby_map);
        setupBottomNavigation();

        txtCafeName = findViewById(R.id.txtCafeName);
        textViewWorkingDay = findViewById(R.id.textViewWorkingDay);
        textViewWorkingHours = findViewById(R.id.textViewWorkingHours);
        txtDistance = findViewById(R.id.txtDistance);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            enableUserLocation();
        }

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof QueryDocumentSnapshot) {
                QueryDocumentSnapshot doc = (QueryDocumentSnapshot) tag;

                String name = doc.getString("name");
                String workingDay = doc.getString("workingDay");
                String formattedWorkingDay = formatWorkingDays(workingDay);
                String workingHours = doc.getString("workingHours");
                Double lat = doc.getDouble("latitude");
                Double lng = doc.getDouble("longitude");
                String statusText = "Status: Unknown";

                if (workingHours != null && workingHours.contains("-")) {
                    try {
                        String[] parts = workingHours.split("-");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                        LocalTime open = LocalTime.parse(parts[0].trim(), formatter);
                        LocalTime close = LocalTime.parse(parts[1].trim(), formatter);
                        LocalTime now = LocalTime.now();

                        boolean isOpen = !now.isBefore(open) && !now.isAfter(close);
                        statusText = isOpen ? "Open Now" : "Closed";
                    } catch (Exception e) {
                        statusText = "Invalid hours";
                    }
                }

                TextView textViewStatus = findViewById(R.id.textViewStatus);
                textViewStatus.setText(statusText);
                textViewStatus.setTextColor(statusText.equals("Open Now") ?
                        ContextCompat.getColor(this, android.R.color.holo_green_dark) :
                        ContextCompat.getColor(this, android.R.color.holo_red_dark));

                txtCafeName.setText(name != null ? name : "-");
                textViewWorkingDay.setText("Working Days: " + (formattedWorkingDay != null ? formattedWorkingDay : "-"));
                textViewWorkingHours.setText("Working Hours: " + (workingHours != null ? workingHours : "-"));

                if (lat != null && lng != null && userLocation != null) {
                    float[] result = new float[1];
                    Location.distanceBetween(
                            userLocation.getLatitude(), userLocation.getLongitude(),
                            lat, lng, result);

                    String distText = new DecimalFormat("#.##").format(result[0] / 1000.0) + " km";
                    txtDistance.setText(distText);
                }

                marker.showInfoWindow();
            }
            return true;
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof QueryDocumentSnapshot) {
                QueryDocumentSnapshot doc = (QueryDocumentSnapshot) tag;
                Double lat = doc.getDouble("latitude");
                Double lng = doc.getDouble("longitude");

                if (lat != null && lng != null) {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");

                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        Toast.makeText(this, "Google Maps app not found.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        userLocation = location;
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13));
                        loadNearbyCafes();
                    } else {
                        Toast.makeText(this, "Unable to detect location.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadNearbyCafes() {
        firestore.collection("cafes").get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        String name = doc.getString("name");

                        if (lat != null && lng != null && name != null) {
                            LatLng cafeLatLng = new LatLng(lat, lng);
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(cafeLatLng)
                                    .title(name));
                            if (marker != null) {
                                marker.setTag(doc);
                            }
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        } else {
            Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
        }
    }
}
