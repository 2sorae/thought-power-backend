-- 생각의힘 / MySQL 8.0.16+ (8.4 compatible), not MariaDB
-- UTF-8. New schema installation only; existing tables are never dropped.
-- DATETIME values are UTC by application convention. Set EVERY app connection to +00:00.
-- diary_date is supplied by the server using Asia/Seoul, not UTC CURRENT_DATE.
SET NAMES utf8mb4;
SET time_zone = '+00:00';
CREATE DATABASE IF NOT EXISTS `thought_power` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs;
USE `thought_power`;

CREATE TABLE `children` (
  `id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  `name` VARCHAR(50) NOT NULL,
  `age` SMALLINT UNSIGNED NOT NULL,
  `age_recorded_at` DATE NOT NULL,
  `guardian_type` VARCHAR(30) NOT NULL,
  `diary_version` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT `ck_children_1` CHECK (`id` = 1),
  CONSTRAINT `ck_children_2` CHECK (`age` BETWEEN 6 AND 10),
  CONSTRAINT `ck_children_3` CHECK (CHAR_LENGTH(TRIM(`name`)) > 0),
  CONSTRAINT `ck_children_4` CHECK (`guardian_type` IN ('PARENT','SCHOOL_TEACHER','FACILITY_TEACHER','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `emotions` (
  `id` SMALLINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  `code` VARCHAR(20) NOT NULL,
  UNIQUE KEY `uq_emotions_code` (`code`),
  `name` VARCHAR(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `diaries` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `child_id` BIGINT UNSIGNED NOT NULL,
  `emotion_id` SMALLINT UNSIGNED NOT NULL,
  `diary_date` DATE NOT NULL,
  `input_type` VARCHAR(10) NOT NULL,
  `content` TEXT NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY `ix_diaries_1` (`child_id`, `diary_date`),
  CONSTRAINT `fk_diaries_child_id` FOREIGN KEY (`child_id`) REFERENCES `children` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_diaries_emotion_id` FOREIGN KEY (`emotion_id`) REFERENCES `emotions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_diaries_1` CHECK (`input_type` IN ('TEXT','VOICE')),
  CONSTRAINT `ck_diaries_2` CHECK (CHAR_LENGTH(TRIM(`content`)) > 0),
  CONSTRAINT `ck_diaries_3` CHECK (`input_type` <> 'TEXT' OR CHAR_LENGTH(`content`) <= 500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `diary_questions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `diary_id` BIGINT UNSIGNED NOT NULL,
  `stage` VARCHAR(20) NOT NULL,
  `sequence` SMALLINT UNSIGNED NOT NULL,
  `question_text` TEXT NOT NULL,
  `answer_text` TEXT NOT NULL,
  UNIQUE KEY `ix_diary_questions_1` (`diary_id`, `stage`),
  UNIQUE KEY `ix_diary_questions_2` (`diary_id`, `sequence`),
  CONSTRAINT `fk_diary_questions_diary_id` FOREIGN KEY (`diary_id`) REFERENCES `diaries` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `ck_diary_questions_1` CHECK ((`stage` = 'EMOTION' AND `sequence` = 1) OR (`stage` = 'REASON' AND `sequence` = 2) OR (`stage` = 'PERSPECTIVE' AND `sequence` = 3)),
  CONSTRAINT `ck_diary_questions_2` CHECK (CHAR_LENGTH(TRIM(`question_text`)) > 0),
  CONSTRAINT `ck_diary_questions_3` CHECK (CHAR_LENGTH(TRIM(`answer_text`)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE `emotion_reports` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `child_id` BIGINT UNSIGNED NOT NULL,
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `source_version` BIGINT UNSIGNED NOT NULL,
  `most_emotion_reason` TEXT NOT NULL,
  `least_emotion_reason` TEXT NOT NULL,
  `parent_advice` TEXT NOT NULL,
  `trend_title` VARCHAR(100) NOT NULL,
  `trend_interpretation` TEXT NOT NULL,
  `generated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY `ix_emotion_reports_1` (`child_id`, `start_date`, `end_date`),
  CONSTRAINT `fk_emotion_reports_child_id` FOREIGN KEY (`child_id`) REFERENCES `children` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `ck_emotion_reports_1` CHECK (`start_date` <= `end_date`),
  CONSTRAINT `ck_emotion_reports_2` CHECK (CHAR_LENGTH(TRIM(`most_emotion_reason`)) > 0),
  CONSTRAINT `ck_emotion_reports_3` CHECK (CHAR_LENGTH(TRIM(`least_emotion_reason`)) > 0),
  CONSTRAINT `ck_emotion_reports_4` CHECK (CHAR_LENGTH(TRIM(`parent_advice`)) > 0),
  CONSTRAINT `ck_emotion_reports_5` CHECK (CHAR_LENGTH(TRIM(`trend_title`)) > 0),
  CONSTRAINT `ck_emotion_reports_6` CHECK (CHAR_LENGTH(TRIM(`trend_interpretation`)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

-- 감정 기준 데이터. 약관은 프론트에서 표시하며 DB에 저장하지 않는다.
INSERT INTO emotions (id, code, name) VALUES
(1, 'JOY', '기쁨'), (2, 'SADNESS', '슬픔'), (3, 'ANGER', '화남'),
(4, 'FEAR', '무서움'), (5, 'SURPRISE', '놀람'), (6, 'SHAME', '부끄러움');

-- Fixed profile: insert children.id=1 with the actual name, age and guardian type at initial setup.
-- No fabricated profile is seeded. All requests use child_id=1 on the server.
-- Voice corrections are client-side only; store the corrected final text with input_type=VOICE.
-- IMPORTANT: save 1 diary + exactly 3 question rows in ONE application transaction.
-- The database enforces stage/order and uniqueness, but not the total row count.
-- Expose no diary UPDATE endpoint; SQL itself does not prohibit administrator UPDATE.
