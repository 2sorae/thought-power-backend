-- Run once on an EXISTING database before deploying empty-answer support.
-- Keep answer_text NOT NULL: omitted/null answers are still invalid; empty strings are allowed.
-- Check the target database and SHOW CREATE TABLE diary_questions first.
ALTER TABLE diary_questions DROP CHECK ck_diary_questions_3;
