-- Phase33-B: add terminal season cancellation and durable club notifications.
-- Existing business rows are intentionally left unchanged; runtime scans perform cancellations.
USE league_ticket;

ALTER TABLE season_info DROP CHECK ck_season_status;

ALTER TABLE season_info
    ADD COLUMN cancel_reason VARCHAR(255) NULL COMMENT '赛季取消原因' AFTER season_status,
    ADD COLUMN cancelled_at DATETIME NULL COMMENT '赛季取消业务时间' AFTER cancel_reason,
    ADD CONSTRAINT ck_season_status CHECK (
        season_status IN ('DRAFT', 'REGISTRATION', 'PREPARING', 'IN_PROGRESS', 'FINISHED', 'CANCELLED')
    ),
    ADD CONSTRAINT ck_season_cancellation CHECK (
        (season_status = 'CANCELLED' AND cancel_reason IS NOT NULL AND cancelled_at IS NOT NULL)
        OR (season_status <> 'CANCELLED' AND cancel_reason IS NULL AND cancelled_at IS NULL)
    );

CREATE TABLE club_season_notification (
    notification_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '俱乐部赛季通知主键',
    season_id BIGINT UNSIGNED NOT NULL COMMENT '关联赛季',
    club_id BIGINT UNSIGNED NOT NULL COMMENT '接收俱乐部',
    notification_type VARCHAR(32) NOT NULL COMMENT '通知类型',
    message VARCHAR(500) NOT NULL COMMENT '通知内容',
    occurred_at DATETIME NOT NULL COMMENT '业务发生时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (notification_id),
    CONSTRAINT uq_club_season_notification UNIQUE (season_id, club_id, notification_type),
    CONSTRAINT fk_club_season_notification_season FOREIGN KEY (season_id) REFERENCES season_info (season_id),
    CONSTRAINT fk_club_season_notification_club FOREIGN KEY (club_id) REFERENCES club_info (club_id),
    CONSTRAINT ck_club_season_notification_type CHECK (notification_type IN ('SEASON_CANCELLED')),
    KEY idx_club_season_notification_query (club_id, occurred_at)
) ENGINE = InnoDB COMMENT = '俱乐部赛季通知';
