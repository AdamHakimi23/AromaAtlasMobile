// File: OwnerEditCafeActivity.java

package com.example.aromaatlas;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerEditCafeActivity extends AppCompatActivity {

    private EditText etCafeName, etDescription, etWorkingDay, etWorkingHours;
    private final String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private final boolean[] selectedDays = new boolean[days.length];
    private ImageView imagePreview;
    private android.widget.Spinner spnCafe;
    private String imageUrl = "";
    private Uri selectedImageUri = null;
    private ProgressDialog progressDialog;
    private Button btnSave, btnEditLocation;
    private String cafeId;
    private FirebaseFirestore db;
    private ImageButton backButton;

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

    // Add this at the top with your fields
    private List<String> cafeIds = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_edit_cafe);

        db = FirebaseFirestore.getInstance();

        spnCafe = findViewById(R.id.spnCafe);
        etCafeName = findViewById(R.id.etCafeName);
        etDescription = findViewById(R.id.etDescription);
        etWorkingDay = findViewById(R.id.etWorkingDay);
        etWorkingHours = findViewById(R.id.etWorkingHours);
        etWorkingHours.setOnClickListener(v -> showTimeRangePicker());
        btnSave = findViewById(R.id.btnSaveCafe);
        btnEditLocation = findViewById(R.id.btnEditLocation);
        imagePreview = findViewById(R.id.imagePreview);
        backButton = findViewById(R.id.backButton);

        cafeId = getIntent().getStringExtra("cafeId");

        backButton.setOnClickListener(v -> finish());


        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            db.collection("cafes")
                    .whereEqualTo("ownerId", currentUser.getUid())
                    .get()
                    .addOnSuccessListener(query -> {
                        List<String> cafeNames = new ArrayList<>();
                        for (DocumentSnapshot doc : query) {
                            String cafeName = doc.getString("name");
                            if (cafeName != null) {
                                cafeNames.add(cafeName);
                                cafeIds.add(doc.getId()); // save matching cafeId
                            }
                        }

                        ArrayAdapter<String> cafeAdapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_spinner_item,
                                cafeNames
                        );
                        cafeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spnCafe.setAdapter(cafeAdapter);

                        // Load the first cafe by default
                        if (!cafeIds.isEmpty()) {
                            cafeId = cafeIds.get(0);
                            loadCafeData(cafeId);
                        }

                        // Spinner listener to switch between cafes
                        spnCafe.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                                cafeId = cafeIds.get(position);
                                loadCafeData(cafeId);
                            }

                            @Override
                            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                                cafeId = null;
                            }
                        });
                    });
        }

        btnEditLocation.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerEditCafeActivity.this, OwnerPinLocationActivity.class);
            intent.putExtra("cafeId", cafeId); // pass selected cafe ID
            startActivity(intent);
        });


        imagePreview.setOnClickListener(v -> pickImage());

        etWorkingDay.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Working Days");
            builder.setMultiChoiceItems(days, selectedDays, (dialog, which, isChecked) -> selectedDays[which] = isChecked);
            builder.setPositiveButton("OK", (dialog, which) -> {
                StringBuilder selected = new StringBuilder();
                for (int i = 0; i < selectedDays.length; i++) {
                    if (selectedDays[i]) {
                        if (selected.length() > 0) selected.append(", ");
                        selected.append(days[i]);
                    }
                }
                etWorkingDay.setText(selected.toString());
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        btnSave.setOnClickListener(v -> updateCafe());

        loadCafeData(cafeId);
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

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToFirebaseStorage(Uri imageUri) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String fileName = "cafe_images/" + System.currentTimeMillis() + ".jpg";
        FirebaseStorage.getInstance().getReference().child(fileName)
                .putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                            imageUrl = uri.toString();
                            progressDialog.dismiss();
                            Toast.makeText(this, "Image uploaded", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCafeData(String cafeId) {
        db.collection("cafes").document(cafeId).get()
                .addOnSuccessListener(documentSnapshot -> {

                    String img = documentSnapshot.getString("imageUrl");
                    if (img != null && !img.isEmpty()) {
                        Glide.with(this).load(img).into(imagePreview); // use Glide or Picasso
                        imageUrl = img; // retain existing URL unless new one picked
                    }

                    if (documentSnapshot.exists()) {
                        etCafeName.setText(documentSnapshot.getString("name"));
                        etDescription.setText(documentSnapshot.getString("description"));
                        etWorkingDay.setText(documentSnapshot.getString("workingDay"));
                        etWorkingHours.setText(documentSnapshot.getString("workingHours"));
                    } else {
                        Toast.makeText(this, "Café not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load café data", Toast.LENGTH_SHORT).show());
    }


    private void updateCafe() {
        String name = etCafeName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String workingDay = etWorkingDay.getText().toString().trim();
        String workingHours = etWorkingHours.getText().toString().trim();

        if (name.isEmpty() || workingDay.isEmpty() || workingHours.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }


        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("name", name);
        updatedData.put("description", description);
        updatedData.put("workingDay", workingDay);
        updatedData.put("workingHours", workingHours);

        if (!imageUrl.isEmpty()) {
            updatedData.put("imageUrl", imageUrl);
        }


        db.collection("cafes").document(cafeId)
                .update(updatedData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Café updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update café", Toast.LENGTH_SHORT).show());

    }
}
