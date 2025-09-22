package com.example.aromaatlas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class StatusActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button checkStatusBtn, reapplyBtn;
    private TextView statusMessage;
    private ImageView statusIcon;
    private FirebaseFirestore db;

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // closes the activity
        return true;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Check Status");
        }


        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        emailInput = findViewById(R.id.emailInput);
        checkStatusBtn = findViewById(R.id.checkStatusBtn);
        statusMessage = findViewById(R.id.statusMessage);
        statusIcon = findViewById(R.id.statusIcon);
        reapplyBtn = findViewById(R.id.reapplyBtn);
        reapplyBtn.setVisibility(View.GONE);


        db = FirebaseFirestore.getInstance();



        reapplyBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterPage.class));
        });

        checkStatusBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Step 1: Check in 'users' collection
            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(userQuerySnapshot -> {
                        if (!userQuerySnapshot.isEmpty()) {
                            DocumentSnapshot userSnap = userQuerySnapshot.getDocuments().get(0);
                            String status = userSnap.getString("status");
                            showStatus(status != null ? status : "unknown");
                        } else {
                            // Step 2: Not in users → check in 'registration'
                            db.collection("registration")
                                    .whereEqualTo("email", email)
                                    .get()
                                    .addOnSuccessListener(regQuerySnapshot -> {
                                        if (!regQuerySnapshot.isEmpty()) {
                                            DocumentSnapshot regSnap = regQuerySnapshot.getDocuments().get(0);
                                            String regStatus = regSnap.getString("status");
                                            showStatus(regStatus != null ? regStatus : "unknown");
                                        } else {
                                            Toast.makeText(this, "No record found with this email.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error checking registration: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error checking users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

    }

    private void showStatus(String status) {
        statusMessage.setVisibility(View.VISIBLE);
        statusIcon.setVisibility(View.VISIBLE);

        if ("approved".equalsIgnoreCase(status)) {
            statusMessage.setText("✅ Your account is approved!\nPlease check your email to reset your password.");
            statusIcon.setImageResource(R.drawable.ic_approved);
            reapplyBtn.setVisibility(View.GONE);
        } else if ("pending".equalsIgnoreCase(status)) {
            statusMessage.setText("⏳ Your registration is pending.");
            statusIcon.setImageResource(R.drawable.ic_pending);
            reapplyBtn.setVisibility(View.GONE);
        } else if ("rejected".equalsIgnoreCase(status)) {
            statusMessage.setText("❌ Your registration was rejected.");
            statusIcon.setImageResource(R.drawable.ic_rejected);
            reapplyBtn.setVisibility(View.VISIBLE); // Show reapply option
        } else {
            statusMessage.setText("Unknown status.");
            statusIcon.setImageResource(R.drawable.ic_pending);
            reapplyBtn.setVisibility(View.GONE);
        }
    }

}
