package com.thoughtpower.domain.repository;

import com.thoughtpower.domain.entity.Emotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmotionRepository extends JpaRepository<Emotion, Short> {

    Optional<Emotion> findByCode(String code);
}