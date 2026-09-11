USE league_ticket;

-- Schema-only migration. Existing seasons, matches, sale windows and scores are preserved.
ALTER TABLE season_info
    ADD COLUMN ticket_sale_start_time DATETIME NULL AFTER registration_deadline,
    DROP CHECK ck_season_max_clubs,
    ADD CONSTRAINT ck_season_max_clubs CHECK (max_clubs IS NULL OR max_clubs BETWEEN 2 AND 20);

ALTER TABLE refund_apply
    ADD COLUMN refund_rate DECIMAL(5,2) NULL AFTER refund_amount,
    ADD COLUMN fee_amount DECIMAL(10,2) UNSIGNED NOT NULL DEFAULT 0 AFTER refund_rate,
    ADD COLUMN processing_mode VARCHAR(16) NOT NULL DEFAULT 'MANUAL' AFTER refund_status,
    DROP CHECK ck_refund_audit_fields,
    ADD CONSTRAINT ck_refund_rate CHECK (refund_rate IS NULL OR refund_rate IN (0.50, 1.00)),
    ADD CONSTRAINT ck_refund_processing_mode CHECK (processing_mode IN ('MANUAL', 'AUTO')),
    ADD CONSTRAINT ck_refund_audit_fields CHECK (
        (refund_status = 'PENDING' AND auditor_id IS NULL AND audit_time IS NULL)
        OR (refund_status IN ('APPROVED', 'REJECTED') AND processing_mode = 'MANUAL' AND auditor_id IS NOT NULL AND audit_time IS NOT NULL)
        OR (refund_status = 'APPROVED' AND processing_mode = 'AUTO' AND auditor_id IS NULL AND audit_time IS NOT NULL)
    );

CREATE TABLE match_result_submission (
    submission_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    match_id BIGINT UNSIGNED NOT NULL,
    event_admin_id BIGINT UNSIGNED NOT NULL,
    home_score INT UNSIGNED NOT NULL,
    away_score INT UNSIGNED NOT NULL,
    submitted_at DATETIME NOT NULL,
    PRIMARY KEY (submission_id),
    CONSTRAINT uq_match_result_submitter UNIQUE (match_id, event_admin_id),
    CONSTRAINT fk_result_submission_match FOREIGN KEY (match_id) REFERENCES match_info (match_id),
    CONSTRAINT fk_result_submission_event_admin FOREIGN KEY (event_admin_id) REFERENCES sys_user (user_id),
    KEY idx_result_submission_match_score (match_id, home_score, away_score)
) ENGINE=InnoDB COMMENT='EVENT_ADMIN独立赛果提交，不相互覆盖';

CREATE TABLE match_result_review (
    match_id BIGINT UNSIGNED NOT NULL,
    review_status VARCHAR(24) NOT NULL DEFAULT 'PENDING_ADMIN_REVIEW',
    review_reason VARCHAR(24) NOT NULL,
    final_home_score INT UNSIGNED NULL,
    final_away_score INT UNSIGNED NULL,
    confirmed_by BIGINT UNSIGNED NULL,
    confirmed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (match_id),
    CONSTRAINT fk_result_review_match FOREIGN KEY (match_id) REFERENCES match_info (match_id),
    CONSTRAINT fk_result_review_admin FOREIGN KEY (confirmed_by) REFERENCES sys_user (user_id),
    CONSTRAINT ck_result_review_status CHECK (review_status IN ('PENDING_ADMIN_REVIEW', 'AUTO_PUBLISHED', 'ADMIN_CONFIRMED')),
    CONSTRAINT ck_result_review_reason CHECK (review_reason IN ('SINGLE_SUBMISSION', 'CONFLICT', 'CONSENSUS', 'ADMIN_DECISION')),
    CONSTRAINT ck_result_review_final_score CHECK ((review_status = 'PENDING_ADMIN_REVIEW' AND final_home_score IS NULL AND final_away_score IS NULL AND confirmed_at IS NULL) OR (review_status IN ('AUTO_PUBLISHED', 'ADMIN_CONFIRMED') AND final_home_score IS NOT NULL AND final_away_score IS NOT NULL AND confirmed_at IS NOT NULL)),
    CONSTRAINT ck_result_review_confirmer CHECK ((review_status = 'ADMIN_CONFIRMED' AND confirmed_by IS NOT NULL) OR (review_status <> 'ADMIN_CONFIRMED' AND confirmed_by IS NULL)),
    KEY idx_result_review_pending (review_status, updated_at)
) ENGINE=InnoDB COMMENT='赛果共识与ADMIN最终确认';
