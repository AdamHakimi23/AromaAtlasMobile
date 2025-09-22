package com.example.aromaatlas;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ResultActivity extends BaseActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_result);
        setupBottomNavigation();

        TextView resultText = findViewById(R.id.resultText);
        Map<String, Integer> beanScores = (HashMap<String, Integer>) getIntent().getSerializableExtra("beanScores");

        // Determine the best bean
        String bestBean = null;
        int maxScore = 0;
        for (Map.Entry<String, Integer> entry : beanScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestBean = entry.getKey();
            }
        }

        resultText.setText("We recommend: " + bestBean + " beans!");

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Save the quiz result to Firestore
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("userId", userId);
        resultData.put("recommendedBean", bestBean);
        resultData.put("beanScores", beanScores);
        resultData.put("timestamp", Timestamp.now());

        db.collection("quiz_results")
                .add(resultData)
                .addOnSuccessListener(docRef -> {
                    // You can add a Toast or Log here if needed
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    e.printStackTrace();
                });
    }
}
