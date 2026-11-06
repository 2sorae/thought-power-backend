package com.thoughtpower.domain.dto;

import java.util.List;

public class QuestionGenerateRequest {

    private String emotion;
    private String content;
    private Integer order;
    private String type;
    private List<PreviousAnswer> previousAnswers;

    public QuestionGenerateRequest() {
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<PreviousAnswer> getPreviousAnswers() {
        return previousAnswers;
    }

    public void setPreviousAnswers(List<PreviousAnswer> previousAnswers) {
        this.previousAnswers = previousAnswers;
    }

    public static class PreviousAnswer {

        private Integer order;
        private String type;
        private String question;
        private String answer;

        public PreviousAnswer() {
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }
}