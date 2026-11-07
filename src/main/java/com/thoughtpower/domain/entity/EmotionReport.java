package com.thoughtpower.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "emotion_reports", uniqueConstraints = @UniqueConstraint(name = "ix_emotion_reports_1", columnNames = {"child_id", "start_date", "end_date"}))
public class EmotionReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long sourceVersion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mostEmotionReason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String leastEmotionReason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String parentAdvice;

    //trend_title 삭제

    @Column(nullable = false, columnDefinition = "TEXT")
    private String trendInterpretation;

    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDateTime generatedAt;

    protected EmotionReport() {
    }

    public Long getId() {
        return id;
    }

    public Child getChild() {
        return child;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Long getSourceVersion() {
        return sourceVersion;
    }

    public String getMostEmotionReason() {
        return mostEmotionReason;
    }

    public String getLeastEmotionReason() {
        return leastEmotionReason;
    }

    public String getParentAdvice() {
        return parentAdvice;
    }



    public String getTrendInterpretation() {
        return trendInterpretation;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
