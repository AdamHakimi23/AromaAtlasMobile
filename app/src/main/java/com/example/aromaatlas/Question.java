package com.example.aromaatlas;

import java.util.List;
import java.util.Map;

public class Question {
    private String questionText;
    private List<String> options;
    private Map<String, String> answerToBean;

    public Question(String questionText, List<String> options, Map<String, String> answerToBean) {
        this.questionText = questionText;
        this.options = options;
        this.answerToBean = answerToBean;
    }

    public String getQuestionText() { return questionText; }
    public List<String> getOptions() { return options; }
    public String getBeanForAnswer(String answer) { return answerToBean.get(answer); }
}
