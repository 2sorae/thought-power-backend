package com.thoughtpower.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "diary_questions", uniqueConstraints = {@UniqueConstraint(name = "ix_diary_questions_1", columnNames = {"diary_id", "stage"}),
        @UniqueConstraint(name = "ix_diary_questions_2", columnNames = {"diary_id", "sequence"})})
public class DiaryQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private QuestionStage stage;

    @Column(name = "`sequence`", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private Short sequence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answerText;

    protected DiaryQuestion() {
    }

    public Long getId() {
        return id;
    }

    public Diary getDiary() {
        return diary;
    }

    public QuestionStage getStage() {
        return stage;
    }

    public Short getSequence() {
        return sequence;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getAnswerText() {
        return answerText;
    }
}
