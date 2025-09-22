// File: OwnerEditMenuActivity.java
package com.example.aromaatlas;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class OwnerEditMenuActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final int STORAGE_PERMISSION_REQUEST = 102;

    private EditText edtName, edtPrice;
    private Spinner spnBeanType, spnDrinkType;
    private ImageView imgPreview;
    private Button btnSave;
    private ImageButton backButton;

    private Uri selectedImageUri;
    private String currentImageUrl = "";
    private String menuItemId;

    private FirebaseFirestore firestore;
    private StorageReference storageRef;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_edit_menu);

        edtName = findViewById(R.id.editName);
        edtPrice = findViewById(R.id.editPrice);
        spnBeanType = findViewById(R.id.spinnerBean);
        spnDrinkType = findViewById(R.id.spinnerDrink);
        imgPreview = findViewById(R.id.imgPreview);
        btnSave = findViewById(R.id.btnSave);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("menuImages");
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating...");
        progressDialog.setCancelable(false);

        ArrayAdapter<CharSequence> beanAdapter = ArrayAdapter.createFromResource(
                this, R.array.bean_types_array, android.R.layout.simple_spinner_item
        );
        beanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnBeanType.setAdapter(beanAdapter);

        ArrayAdapter<CharSequence> drinkAdapter = ArrayAdapter.createFromResource(
                this, R.array.drink_types_array, android.R.layout.simple_spinner_item
        );
        drinkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDrinkType.setAdapter(drinkAdapter);

        menuItemId = getIntent().getStringExtra("menuItemId");
        if (menuItemId == null || menuItemId.isEmpty()) {
            Toast.makeText(this, "Invalid menu item ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadItemData();

        imgPreview.setOnClickListener(v -> checkStoragePermission());
        btnSave.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImageAndSave();
            } else {
                saveChanges(currentImageUrl);
            }
        });
    }

    private void loadItemData() {
        firestore.collection("drinks").document(menuItemId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        edtName.setText(doc.getString("name"));
                        edtPrice.setText(String.valueOf(doc.getDouble("price")));

                        currentImageUrl = doc.getString("image");
                        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(currentImageUrl)
                                    .placeholder(R.drawable.placeholder)
                                    .into(imgPreview);
                        } else {
                            imgPreview.setImageResource(R.drawable.placeholder);
                        }

                        setSpinnerSelection(spnBeanType, doc.getString("bean"));
                        setSpinnerSelection(spnDrinkType, doc.getString("drink"));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load item", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int pos = adapter.getPosition(value);
        if (pos >= 0) spinner.setSelection(pos);
    }

    private void checkStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, STORAGE_PERMISSION_REQUEST);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imgPreview.setImageURI(selectedImageUri);
        }
    }

    private void uploadImageAndSave() {
        progressDialog.show();

        String filename = menuItemId + "_" + System.currentTimeMillis();
        StorageReference imageRef = storageRef.child(filename);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(task -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String newImageUrl = uri.toString();
                            saveChanges(newImageUrl);
                        }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveChanges(String imageUrl) {
        String name = edtName.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();
        String bean = spnBeanType.getSelectedItem().toString();
        String drink = spnDrinkType.getSelectedItem().toString();

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("name", name);
        update.put("price", price);
        update.put("bean", bean);
        update.put("drink", drink);
        update.put("image", imageUrl);

        firestore.collection("drinks").document(menuItemId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Menu updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == STORAGE_PERMISSION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
