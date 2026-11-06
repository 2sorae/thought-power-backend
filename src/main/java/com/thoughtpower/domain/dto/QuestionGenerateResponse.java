package com.thoughtpower.domain.dto;

public class QuestionGenerateResponse {

    private Integer order;
    private String type;
    private String content;
    private boolean isFallback;

    public QuestionGenerateResponse(
            Integer order,
            String type,
            String content,
            boolean isFallback
    ) {
        this.order = order;
        this.type = type;
        this.content = content;
        this.isFallback = isFallback;
    }

    public Integer getOrder() {
        return order;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isFallback")
    public boolean isFallback() {
        return isFallback;
    }
}
