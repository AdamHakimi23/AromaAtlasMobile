package com.example.aromaatlas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class QuizActivity extends BaseActivity {
    private List<Question> questionList;
    private int currentIndex = 0;
    private Map<String, Integer> beanScores = new HashMap<>();

    private TextView questionText;
    private RadioGroup optionsGroup;
    private Button nextBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_quiz);
        setupBottomNavigation();

        questionText = findViewById(R.id.questionText);
        optionsGroup = findViewById(R.id.optionsGroup);
        nextBtn = findViewById(R.id.nextButton);

        setupQuestions();
        showQuestion();

        nextBtn.setOnClickListener(v -> {
            int selectedId = optionsGroup.getCheckedRadioButtonId();
            if (selectedId == -1) return;

            RadioButton selectedBtn = findViewById(selectedId);
            String selectedAnswer = selectedBtn.getText().toString();
            String bean = questionList.get(currentIndex).getBeanForAnswer(selectedAnswer);
            beanScores.put(bean, beanScores.getOrDefault(bean, 0) + 1);

            currentIndex++;
            if (currentIndex < questionList.size()) {
                showQuestion();
            } else {
                Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
                intent.putExtra("beanScores", new HashMap<>(beanScores));
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupQuestions() {
        questionList = new ArrayList<>();

        Map<String, String> q1Map = new HashMap<>();
        q1Map.put("Strong", "Robusta");
        q1Map.put("Smooth", "Arabica");
        q1Map.put("Fruity", "Liberica");
        questionList.add(new Question("How do you like your coffee?", Arrays.asList("Strong", "Smooth", "Fruity"), q1Map));

        Map<String, String> q2Map = new HashMap<>();
        q2Map.put("Morning", "Arabica");
        q2Map.put("Afternoon", "Liberica");
        q2Map.put("Night", "Robusta");
        questionList.add(new Question("When do you usually drink coffee?", Arrays.asList("Morning", "Afternoon", "Night"), q2Map));

        Map<String, String> q3Map = new HashMap<>();
        q3Map.put("Sweet", "Liberica");
        q3Map.put("Bitter", "Robusta");
        q3Map.put("Balanced", "Arabica");
        questionList.add(new Question("What flavor profile do you prefer?", Arrays.asList("Sweet", "Bitter", "Balanced"), q3Map));
    }

    private void showQuestion() {
        Question q = questionList.get(currentIndex);
        questionText.setText(q.getQuestionText());

        optionsGroup.removeAllViews();
        for (String option : q.getOptions()) {
            RadioButton rb = new RadioButton(this);
            rb.setText(option);
            optionsGroup.addView(rb);
        }
    }
}
