package com.thoughtpower.domain.repository;

import com.thoughtpower.domain.entity.DiaryQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryQuestionRepository
        extends JpaRepository<DiaryQuestion, Long> {
}