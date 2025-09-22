package com.example.aromaatlas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterPage extends AppCompatActivity {

    private EditText registerEmail, registerPassword, registerLicense;
    private RadioButton radioOwner;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RadioGroup roleGroup;
    private Button registerBtn;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);

        registerEmail = findViewById(R.id.registerEmail);
        registerPassword = findViewById(R.id.registerPassword);
        registerLicense = findViewById(R.id.registerLicense);
        radioOwner = findViewById(R.id.radioOwner);
        roleGroup = findViewById(R.id.roleGroup);
        registerBtn = findViewById(R.id.registerBtn);
        backButton = findViewById(R.id.backButton);

        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioOwner) {
                registerLicense.setVisibility(View.VISIBLE);
            } else {
                registerLicense.setVisibility(View.GONE);
            }
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registerBtn.setOnClickListener(v -> {
            String email = registerEmail.getText().toString().trim();
            String password = registerPassword.getText().toString().trim();
            String SSM = registerLicense.getText().toString().trim();
            String role = radioOwner.isChecked() ? "owner" : "enthusiast";

            // Common validation
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Email and Password are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // SSM required only for business owners
            if (role.equals("owner") && TextUtils.isEmpty(SSM)) {
                Toast.makeText(this, "SSM is required for business owners", Toast.LENGTH_SHORT).show();
                return;
            }

            if (role.equals("owner")) {
                // Owner → Firestore only, no FirebaseAuth yet
                Map<String, Object> userData = new HashMap<>();
                userData.put("email", email);
                userData.put("role", role);
                userData.put("SSM", SSM);
                userData.put("status", "pending");

                db.collection("registration").document(email)
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Registration submitted. Awaiting admin approval.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            } else {
                // Enthusiast → Create Auth + Firestore immediately
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            String uid = authResult.getUser().getUid();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("role", role);
                            userData.put("userId", uid);
                            userData.put("status", "active");

                            db.collection("users").document(uid)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Account created. Welcome, enthusiast!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Auth error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

       // Place this outside registerBtn listener
        backButton.setOnClickListener(v -> finish());
    }
}
