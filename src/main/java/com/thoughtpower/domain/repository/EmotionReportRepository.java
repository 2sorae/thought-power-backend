package com.thoughtpower.domain.repository;

import com.thoughtpower.domain.entity.EmotionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmotionReportRepository
        extends JpaRepository<EmotionReport, Long> {

    @Modifying
    @Query("delete from EmotionReport r where r.child.id = :childId")
    void deleteReportsByChildId(@Param("childId") Long childId);
}