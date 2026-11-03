package com.thoughtpower.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "emotions")
public class Emotion {
    @Id
    @Column(columnDefinition = "SMALLINT UNSIGNED")
    private Short id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 20)
    private String name;

    protected Emotion() {
    }

    public Short getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
