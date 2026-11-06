package com.thoughtpower.domain.repository;

import com.thoughtpower.domain.entity.Child;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChildRepository extends JpaRepository<Child, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Child c where c.id = :id")
    Optional<Child> findForUpdate(@Param("id") Long id);
}