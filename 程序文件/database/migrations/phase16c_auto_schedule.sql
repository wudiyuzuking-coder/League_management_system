-- 阶段16C：在已完成阶段16B迁移的数据库上增加自动排赛批次模型。
USE league_ticket;

CREATE TABLE IF NOT EXISTS season_schedule_batch (
    batch_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    season_id BIGINT UNSIGNED NOT NULL,
    batch_status VARCHAR(16) NOT NULL DEFAULT 'GENERATED',
    trigger_type VARCHAR(16) NOT NULL,
    club_count INT UNSIGNED NOT NULL,
    round_count INT UNSIGNED NOT NULL,
    match_count INT UNSIGNED NOT NULL,
    generated_at DATETIME NOT NULL,
    confirmed_at DATETIME NULL,
    confirmed_by BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (batch_id),
    CONSTRAINT uq_schedule_batch_season UNIQUE (season_id),
    CONSTRAINT fk_schedule_batch_season FOREIGN KEY (season_id) REFERENCES season_info (season_id),
    CONSTRAINT fk_schedule_batch_confirmer FOREIGN KEY (confirmed_by) REFERENCES sys_user (user_id) ON DELETE SET NULL,
    CONSTRAINT ck_schedule_batch_status CHECK (batch_status IN ('GENERATED', 'CONFIRMED')),
    CONSTRAINT ck_schedule_trigger_type CHECK (trigger_type IN ('FULL', 'DEADLINE', 'MANUAL')),
    CONSTRAINT ck_schedule_counts CHECK (club_count >= 2 AND round_count > 0 AND match_count > 0),
    KEY idx_schedule_batch_status_time (batch_status, generated_at)
) ENGINE=InnoDB COMMENT='赛季自动排赛批次及确认状态';

CREATE TABLE IF NOT EXISTS season_schedule_match (
    batch_id BIGINT UNSIGNED NOT NULL,
    match_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (batch_id, match_id),
    CONSTRAINT uq_schedule_match UNIQUE (match_id),
    CONSTRAINT fk_schedule_match_batch FOREIGN KEY (batch_id) REFERENCES season_schedule_batch (batch_id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_match_match FOREIGN KEY (match_id) REFERENCES match_info (match_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='自动排赛批次与比赛关系';

INSERT INTO sys_config(config_key,config_value,value_type,description,config_status)
VALUES('AUTO_SCHEDULE_DEFAULT_KICKOFF_TIME','19:30','STRING','自动排赛默认开球时间（HH:mm）','ENABLED')
ON DUPLICATE KEY UPDATE description=VALUES(description);
