package com.example.aromaatlas;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OwnerDashboardActivity extends BaseActivity {

    private EditText etOwnerName,etCafeName, etDescription, etWorkingDay, etWorkingHours;
    private final String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private final boolean[] selectedDays = new boolean[days.length];
    private Button btnSaveCafe;
    private ImageView imagePreview;
    private String imageUrl = "";
    private Uri selectedImageUri = null;
    private ProgressDialog progressDialog;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imagePreview.setImageURI(selectedImageUri);
                    uploadImageToFirebaseStorage(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_owner_dashboard);
        setupBottomNavigation();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginPage.class));
            finish();
            return;
        }

        etOwnerName = findViewById(R.id.etOwnerName);
        etCafeName = findViewById(R.id.etCafeName);
        etDescription = findViewById(R.id.etDescription);
        etWorkingDay = findViewById(R.id.etWorkingDay);
        etWorkingDay.setOnClickListener(v -> showWorkingDaysDialog());
        etWorkingHours = findViewById(R.id.etWorkingHours);
        etWorkingHours.setOnClickListener(v -> showTimeRangePicker());
        imagePreview = findViewById(R.id.imagePreview);
        imagePreview.setOnClickListener(v -> openImagePicker());

        btnSaveCafe = findViewById(R.id.btnSaveCafe);
        btnSaveCafe.setOnClickListener(v -> {
            if (!imageUrl.isEmpty()) {
                saveCafeToFirestore();
            } else {
                Toast.makeText(this, "Please select and upload a cafe image.", Toast.LENGTH_SHORT).show();
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading cafe...");
        progressDialog.setCancelable(false);

    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToFirebaseStorage(Uri imageUri) {
        if (imageUri == null) return;

        progressDialog.show();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String ownerId = user.getUid();
        String filename = "cafeImages/" + ownerId + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(filename);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            imageUrl = uri.toString();
                            progressDialog.dismiss();
                        }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchCafeAndGoToMenu() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("cafes")
                    .whereEqualTo("ownerId", user.getUid())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String cafeId = querySnapshot.getDocuments().get(0).getId();

                            // ✅ FIX HERE: pass ownerId too
                            Intent intent = new Intent(this, OwnerMenuDashboardActivity.class);
                            intent.putExtra("cafeId", cafeId);
                            intent.putExtra("ownerId", user.getUid()); // ✅ REQUIRED
                            startActivity(intent);

                        } else {
                            Toast.makeText(this, "No café found for user", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    private void showWorkingDaysDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Working Days");

        builder.setMultiChoiceItems(days, selectedDays, (dialog, indexSelected, isChecked) -> {
            selectedDays[indexSelected] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, id) -> {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < days.length; i++) {
                if (selectedDays[i]) {
                    if (result.length() > 0) result.append(", ");
                    result.append(days[i]);
                }
            }
            etWorkingDay.setText(result.toString());
        });

        builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());
        builder.show();
    }

    private void showTimeRangePicker() {
        TimePickerDialog startPicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String startTime = String.format("%02d:%02d", hourOfDay, minute);
            TimePickerDialog endPicker = new TimePickerDialog(this, (view2, hourOfDay2, minute2) -> {
                String endTime = String.format("%02d:%02d", hourOfDay2, minute2);
                etWorkingHours.setText(startTime + " - " + endTime);
            }, 9, 0, true);
            endPicker.setTitle("Select End Time");
            endPicker.show();
        }, 8, 0, true);
        startPicker.setTitle("Select Start Time");
        startPicker.show();
    }

    private void saveCafeToFirestore() {
        String ownerName = etOwnerName.getText().toString().trim();
        String name = etCafeName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String workingDay = etWorkingDay.getText().toString().trim();
        String workingHours = etWorkingHours.getText().toString().trim();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (ownerName.isEmpty() || name.isEmpty() || workingDay.isEmpty() || workingHours.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String rawPrefix = name.replaceAll("\\s+", "").toUpperCase();
        final String cafePrefix = rawPrefix.length() >= 3 ? rawPrefix.substring(0, 3) : rawPrefix;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("cafes")
                .whereGreaterThanOrEqualTo("cafeId", cafePrefix + "-")
                .whereLessThanOrEqualTo("cafeId", cafePrefix + "-\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    int maxIndex = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String id = doc.getString("cafeId");
                        if (id != null && id.startsWith(cafePrefix + "-")) {
                            try {
                                int index = Integer.parseInt(id.substring(id.lastIndexOf("-") + 1));
                                if (index > maxIndex) maxIndex = index;
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    final String newCafeId = cafePrefix + "-" + String.format("%02d", maxIndex + 1);

                    Map<String, Object> cafeData = new HashMap<>();
                    cafeData.put("OwnerName", ownerName);
                    cafeData.put("name", name);
                    cafeData.put("description", description);
                    cafeData.put("workingDay", workingDay);
                    cafeData.put("workingHours", workingHours);
                    cafeData.put("imageUrl", imageUrl);
                    cafeData.put("cafeId", newCafeId);
                    cafeData.put("ownerId", user.getUid());

                    db.collection("cafes")
                            .document(newCafeId) // ← using custom ID as document ID
                            .set(cafeData)       // ← replaces .add()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Café registered!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, OwnerPinLocationActivity.class);
                                intent.putExtra("cafeId", newCafeId);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to save café.", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking existing cafes.", Toast.LENGTH_SHORT).show());
    }
}

