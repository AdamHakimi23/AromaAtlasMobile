// File: OwnerAddMenuActivity.java
package com.example.aromaatlas;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.*;

public class OwnerAddMenuActivity extends AppCompatActivity{

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int STORAGE_PERMISSION_REQUEST = 101;

    private EditText edtMenuName, edtPrice;
    private Spinner spnDrinkType,spnBeanType,Spinner,spnCafe;
    private ImageView imgPreview;
    private Button btnUploadImage, btnSaveMenu;
    private ImageButton backButton;

    private Uri selectedImageUri;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;
    private ProgressDialog progressDialog;
    private List<String> cafeIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_add_menu); // ✅ inserts your screen into BaseActivity's content_frame

        spnCafe = findViewById(R.id.spnCafe);
        edtMenuName = findViewById(R.id.edtMenuName);
        spnBeanType = findViewById(R.id.spnBeanType);
        edtPrice = findViewById(R.id.edtPrice);
        spnDrinkType = findViewById(R.id.spnDrinkType);
        imgPreview = findViewById(R.id.imgPreview);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnSaveMenu = findViewById(R.id.btnSaveMenu);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("menuImages");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading product...");
        progressDialog.setCancelable(false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            firestore.collection("cafes")
                    .whereEqualTo("ownerId", currentUser.getUid())
                    .get()
                    .addOnSuccessListener(query -> {
                        List<String> cafeNames = new ArrayList<>();
                        for (DocumentSnapshot doc : query) {
                            String cafeName = doc.getString("name");
                            if (cafeName != null) {
                                cafeNames.add(cafeName);
                                cafeIds.add(doc.getId()); // save cafeId
                            }
                        }

                        ArrayAdapter<String> cafeAdapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_spinner_item,
                                cafeNames
                        );
                        cafeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spnCafe.setAdapter(cafeAdapter);
                    });
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.drink_types_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDrinkType.setAdapter(adapter);

        ArrayAdapter<CharSequence> beanAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.bean_types_array,
                android.R.layout.simple_spinner_item
        );
        beanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnBeanType.setAdapter(beanAdapter);


        btnUploadImage.setOnClickListener(v -> checkStoragePermission());
        btnSaveMenu.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImageAndSaveMenu();
            } else {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            }
        });
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
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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

    private void uploadImageAndSaveMenu() {
        progressDialog.show();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "You must be logged in to upload.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        String filename = UUID.randomUUID().toString();
        StorageReference fileRef = FirebaseStorage.getInstance()
                .getReference("menuImages/" + userId + "/" + filename);

        fileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(uri ->
                                saveMenuToFirestore(uri.toString())
                        )
                )
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveMenuToFirestore(String imageUrl) {
        String name = edtMenuName.getText().toString().trim();
        String drink = spnDrinkType.getSelectedItem() != null ? spnDrinkType.getSelectedItem().toString().trim() : "";
        String bean = spnBeanType.getSelectedItem() != null ? spnBeanType.getSelectedItem().toString().trim() : "";
        String priceStr = edtPrice.getText().toString().trim();
        int cafeIndex = spnCafe.getSelectedItemPosition();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (cafeIndex == -1 || cafeIds.size() <= cafeIndex) {
            progressDialog.dismiss();
            Toast.makeText(this, "Please select a café.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.isEmpty() || bean.isEmpty() || drink.isEmpty() || priceStr.isEmpty()) {
            progressDialog.dismiss();
            Toast.makeText(this, "Please fill all details.", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Invalid price entered.", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedCafeId = cafeIds.get(cafeIndex);

        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("bean", bean);
        product.put("drink", drink);
        product.put("price", price);
        product.put("image", imageUrl);
        product.put("ownerId", user != null ? user.getUid() : "anonymous");
        product.put("cafeId", selectedCafeId);

        firestore.collection("drinks")
                .add(product)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to add product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

