package com.example.aromaatlas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginPage extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView forgotPasswordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        Button loginBtn = findViewById(R.id.loginBtn);
        Button goToRegisterBtn = findViewById(R.id.goToRegisterBtn);
        Button OwnerApplication = findViewById(R.id.OwnerApplication);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loginBtn.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();

                            db.collection("users").whereEqualTo("userId", userId)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (!querySnapshot.isEmpty()) {
                                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                            String role = document.getString("role");

                                            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                                            prefs.edit().putString("user_role", role).apply();

                                            if ("owner".equals(role)) {
                                                startActivity(new Intent(this, OwnerHomePageActivity.class));
                                            } else {
                                                startActivity(new Intent(this, MainActivity.class));
                                            }
                                            finish();
                                        } else {
                                            Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to fetch user role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        goToRegisterBtn.setOnClickListener(v -> startActivity(new Intent(this, RegisterPage.class)));

        OwnerApplication.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));

        forgotPasswordText.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}
