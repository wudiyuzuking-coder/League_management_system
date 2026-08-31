-- 阶段16B：已有 league_ticket 数据库的一次性最小升级脚本。
-- 新字段保持 NULL 以兼容历史赛季；执行后须由人工为可报名赛季回填明确值。
USE league_ticket;

ALTER TABLE season_info
    ADD COLUMN registration_start_time DATETIME NULL COMMENT '报名开始时间，历史赛季可为空' AFTER end_date,
    ADD COLUMN registration_deadline DATETIME NULL COMMENT '报名截止时间，历史赛季可为空' AFTER registration_start_time,
    ADD COLUMN max_clubs INT UNSIGNED NULL COMMENT '最大报名俱乐部数，历史赛季可为空' AFTER registration_deadline,
    ADD CONSTRAINT ck_season_registration_dates CHECK (registration_deadline IS NULL OR registration_start_time IS NULL OR registration_deadline >= registration_start_time),
    ADD CONSTRAINT ck_season_max_clubs CHECK (max_clubs IS NULL OR max_clubs BETWEEN 1 AND 20);

CREATE TABLE club_season_enrollment (
    enrollment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    season_id BIGINT UNSIGNED NOT NULL,
    club_id BIGINT UNSIGNED NOT NULL,
    stadium_id BIGINT UNSIGNED NOT NULL,
    enrollment_status VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (enrollment_id),
    CONSTRAINT uq_enrollment_season_club UNIQUE (season_id, club_id),
    CONSTRAINT fk_enrollment_season FOREIGN KEY (season_id) REFERENCES season_info (season_id),
    CONSTRAINT fk_enrollment_club FOREIGN KEY (club_id) REFERENCES club_info (club_id),
    CONSTRAINT fk_enrollment_stadium FOREIGN KEY (stadium_id) REFERENCES stadium_info (stadium_id),
    CONSTRAINT ck_enrollment_status CHECK (enrollment_status IN ('SUBMITTED')),
    KEY idx_enrollment_admin_filter (season_id, club_id, enrollment_status, submitted_at)
) ENGINE=InnoDB COMMENT='俱乐部赛季报名';

CREATE TABLE club_season_enrollment_player (
    enrollment_player_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT UNSIGNED NOT NULL,
    player_id BIGINT UNSIGNED NOT NULL,
    lineup_role VARCHAR(16) NOT NULL,
    player_name_snapshot VARCHAR(80) NOT NULL,
    shirt_no_snapshot INT UNSIGNED NULL,
    position_snapshot VARCHAR(20) NOT NULL,
    birth_date_snapshot DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (enrollment_player_id),
    CONSTRAINT uq_enrollment_player UNIQUE (enrollment_id, player_id),
    CONSTRAINT uq_enrollment_shirt UNIQUE (enrollment_id, shirt_no_snapshot),
    CONSTRAINT fk_enrollment_player_enrollment FOREIGN KEY (enrollment_id) REFERENCES club_season_enrollment (enrollment_id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_player_player FOREIGN KEY (player_id) REFERENCES player_info (player_id),
    CONSTRAINT ck_enrollment_lineup_role CHECK (lineup_role IN ('STARTER', 'SUBSTITUTE')),
    CONSTRAINT ck_enrollment_player_position CHECK (position_snapshot IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD'))
) ENGINE=InnoDB COMMENT='赛季报名球员阵容及快照';

CREATE TABLE club_season_enrollment_coach (
    enrollment_coach_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT UNSIGNED NOT NULL,
    coach_id BIGINT UNSIGNED NOT NULL,
    coach_name_snapshot VARCHAR(80) NOT NULL,
    title_snapshot VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (enrollment_coach_id),
    CONSTRAINT uq_enrollment_coach UNIQUE (enrollment_id, coach_id),
    CONSTRAINT fk_enrollment_coach_enrollment FOREIGN KEY (enrollment_id) REFERENCES club_season_enrollment (enrollment_id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_coach_coach FOREIGN KEY (coach_id) REFERENCES coach_info (coach_id)
) ENGINE=InnoDB COMMENT='赛季报名教练及快照';

-- 示例（请按实际赛季业务时间人工填写，禁止自动推断）：
-- UPDATE season_info SET registration_start_time='2027-01-01 00:00:00',
--   registration_deadline='2027-02-21 00:00:00', max_clubs=16 WHERE season_id=?;
