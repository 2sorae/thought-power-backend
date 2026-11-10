package com.thoughtpower.domain.repository;

import com.thoughtpower.domain.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    boolean existsByChild_IdAndDiaryDate(
            Long childId,
            LocalDate diaryDate
    );
}