package com.thoughtpower.domain.dto;

import java.util.List;
import java.util.UUID;

public class QuestionFlowResponse {
    private final UUID flowId;
    private final String action;
    private final int questionVersion;
    private final int retryCount;
    private final Boolean informative;
    private final QuestionGenerateResponse question;
    private final List<DiaryCreateRequest.QuestionAnswer> questions;

    public QuestionFlowResponse(UUID flowId, String action, int questionVersion, int retryCount,
            Boolean informative, QuestionGenerateResponse question, List<DiaryCreateRequest.QuestionAnswer> questions) {
        this.flowId = flowId;
        this.action = action;
        this.questionVersion = questionVersion;
        this.retryCount = retryCount;
        this.informative = informative;
        this.question = question;
        this.questions = List.copyOf(questions);
    }
    public UUID getFlowId() { return flowId; }
    public String getAction() { return action; }
    public int getQuestionVersion() { return questionVersion; }
    public int getRetryCount() { return retryCount; }
    public Boolean getInformative() { return informative; }
    public QuestionGenerateResponse getQuestion() { return question; }
    public List<DiaryCreateRequest.QuestionAnswer> getQuestions() { return questions; }
}
