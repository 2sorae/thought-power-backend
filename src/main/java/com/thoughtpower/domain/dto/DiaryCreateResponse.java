package com.thoughtpower.domain.dto;

import java.time.LocalDate;

public class DiaryCreateResponse {

    private final Long diaryId;
    private final LocalDate date;
    private final boolean completed;

    public DiaryCreateResponse(Long diaryId, LocalDate date) {
        this.diaryId = diaryId;
        this.date = date;
        this.completed = true;
    }

    public Long getDiaryId() {
        return diaryId;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isCompleted() {
        return completed;
    }
}