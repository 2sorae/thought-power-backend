package com.thoughtpower.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class QuestionAnswerSubmitRequest {
    @NotNull
    @Min(0)
    private Integer questionVersion;
    @NotNull
    private String answer;
    public QuestionAnswerSubmitRequest() {}
    public Integer getQuestionVersion() { return questionVersion; }
    public void setQuestionVersion(Integer questionVersion) { this.questionVersion = questionVersion; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
}
