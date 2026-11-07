package com.thoughtpower.domain.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class QuestionGenerateRequest {

    @NotBlank
    @Pattern(regexp = "JOY|SADNESS|ANGER|FEAR|SURPRISE|SHAME")
    private String emotion;
    @NotBlank
    private String content;
    @NotNull
    @Min(1)
    @Max(3)
    private Integer order;
    @NotBlank
    @Pattern(regexp = "EMOTION|REASON|PERSPECTIVE")
    private String type;
    @NotNull
    @Size(max = 2)
    private List<@NotNull @Valid PreviousAnswer> previousAnswers;

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

        @NotNull
        @Min(1)
        @Max(3)
        private Integer order;
        @NotBlank
        @Pattern(regexp = "EMOTION|REASON|PERSPECTIVE")
        private String type;
        @NotBlank
        private String question;
        @NotBlank
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