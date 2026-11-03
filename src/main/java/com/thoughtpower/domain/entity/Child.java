package com.thoughtpower.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "children")
public class Child {
    @Id
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id = 1L;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private Short age;

    @Column(nullable = false)
    private LocalDate ageRecordedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private GuardianType guardianType;

    @Column(nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long diaryVersion = 0L;

    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Child() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Short getAge() {
        return age;
    }

    public LocalDate getAgeRecordedAt() {
        return ageRecordedAt;
    }

    public GuardianType getGuardianType() {
        return guardianType;
    }

    public Long getDiaryVersion() {
        return diaryVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
