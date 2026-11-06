package com.thoughtpower.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "diaries", uniqueConstraints = @UniqueConstraint(name = "ix_diaries_1", columnNames = {"child_id", "diary_date"}))
public class Diary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emotion_id", nullable = false)
    private Emotion emotion;

    @Column(nullable = false)
    private LocalDate diaryDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private InputType inputType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Diary() {
    }

    public Long getId() {
        return id;
    }

    public Child getChild() {
        return child;
    }

    public Emotion getEmotion() {
        return emotion;
    }

    public LocalDate getDiaryDate() {
        return diaryDate;
    }

    public InputType getInputType() {
        return inputType;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public Diary(
            Child child,
            Emotion emotion,
            LocalDate diaryDate,
            InputType inputType,
            String content
    ) {
        this.child = child;
        this.emotion = emotion;
        this.diaryDate = diaryDate;
        this.inputType = inputType;
        this.content = content;
    }
}
