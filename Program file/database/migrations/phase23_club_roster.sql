-- Phase23 CLUB roster extensions.
-- Schema only: no historical UPDATE/DELETE/cleanup and no inferred lineup roles.

ALTER TABLE player_info
    ADD COLUMN birth_year SMALLINT UNSIGNED NULL COMMENT '新流程出生年份；历史完整生日原样保留' AFTER birth_date,
    ADD COLUMN lineup_role VARCHAR(16) NULL COMMENT '当前阵容角色；历史数据不自动推断' AFTER player_status,
    ADD CONSTRAINT ck_player_birth_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2100),
    ADD CONSTRAINT ck_player_lineup_role CHECK (lineup_role IS NULL OR lineup_role IN ('STARTER', 'SUBSTITUTE'));

ALTER TABLE coach_info
    ADD COLUMN birth_year SMALLINT UNSIGNED NULL COMMENT '新流程出生年份' AFTER nationality,
    ADD CONSTRAINT ck_coach_birth_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2100);

ALTER TABLE club_season_enrollment_player
    ADD COLUMN birth_year_snapshot SMALLINT UNSIGNED NULL COMMENT '报名时出生年份快照' AFTER birth_date_snapshot,
    ADD COLUMN nationality_snapshot VARCHAR(50) NULL COMMENT '报名时国籍快照' AFTER birth_year_snapshot;

ALTER TABLE club_season_enrollment_coach
    ADD COLUMN birth_year_snapshot SMALLINT UNSIGNED NULL COMMENT '报名时出生年份快照' AFTER title_snapshot,
    ADD COLUMN nationality_snapshot VARCHAR(50) NULL COMMENT '报名时国籍快照' AFTER birth_year_snapshot;
