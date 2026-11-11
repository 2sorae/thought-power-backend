package com.thoughtpower.domain.dto;

import com.thoughtpower.domain.entity.InputType;
import com.thoughtpower.domain.entity.QuestionStage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public class DiaryCreateRequest {

    @NotBlank
    @Pattern(regexp = "JOY|SADNESS|ANGER|FEAR|SURPRISE|SHAME")
    private String emotion;

    @NotNull
    private InputType inputType;

    @NotBlank
    private String content;

    @NotNull
    @Size(min = 3, max = 3)
    private List<@NotNull @Valid QuestionAnswer> questions;

    public DiaryCreateRequest() {
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<QuestionAnswer> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionAnswer> questions) {
        this.questions = questions;
    }

    public static class QuestionAnswer {

        @NotNull
        @Min(1)
        @Max(3)
        private Integer order;

        @NotNull
        private QuestionStage type;

        @NotBlank
        private String content;

        @NotNull
        private String answer;

        public QuestionAnswer() {
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }

        public QuestionStage getType() {
            return type;
        }

        public void setType(QuestionStage type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }
}