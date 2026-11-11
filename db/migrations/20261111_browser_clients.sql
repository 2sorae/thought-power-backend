-- EXISTING MySQL 8 DB only. Run once BEFORE deploying browser identity support.
-- Back up first. Stop app writes during this migration; MySQL DDL auto-commits.
-- Verify SHOW CREATE TABLE for children, diaries and emotion_reports first.
-- Existing records are retained. Legacy client_id stays NULL (no shared fallback).
-- Foreign keys must be temporarily removed to enable identity generation on children.id.
ALTER TABLE diaries DROP FOREIGN KEY fk_diaries_child_id;
ALTER TABLE emotion_reports DROP FOREIGN KEY fk_emotion_reports_child_id;
ALTER TABLE children
    DROP CHECK ck_children_1,
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ADD COLUMN client_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD UNIQUE KEY uq_children_client_id (client_id);
ALTER TABLE diaries ADD CONSTRAINT fk_diaries_child_id
    FOREIGN KEY (child_id) REFERENCES children(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE emotion_reports ADD CONSTRAINT fk_emotion_reports_child_id
    FOREIGN KEY (child_id) REFERENCES children(id) ON DELETE CASCADE ON UPDATE RESTRICT;
-- Confirm BOTH foreign keys are restored before restarting the app.
-- If interrupted, inspect the schema and resume remaining steps; do not blindly rerun.
