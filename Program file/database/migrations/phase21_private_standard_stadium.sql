-- Phase21：只扩展标准私有主场 schema。
-- 历史 LEGACY 场馆、票区、座位、比赛票区和库存均不转换、不拆分、不重绑。

USE league_ticket;

ALTER TABLE stadium_info
    ADD COLUMN venue_model VARCHAR(16) NOT NULL DEFAULT 'LEGACY' COMMENT '场馆模型：历史兼容或标准8票区' AFTER layout_description,
    ADD CONSTRAINT ck_stadium_venue_model CHECK (venue_model IN ('LEGACY', 'STANDARD_8'));

ALTER TABLE stadium_zone
    ADD COLUMN zone_direction VARCHAR(8) NULL COMMENT '标准票区方向' AFTER zone_name,
    ADD COLUMN ticket_type VARCHAR(8) NULL COMMENT '标准票区类型' AFTER zone_direction,
    ADD CONSTRAINT uq_stadium_zone_direction_type UNIQUE (stadium_id, zone_direction, ticket_type),
    ADD CONSTRAINT ck_stadium_zone_classification CHECK (
        (zone_direction IS NULL AND ticket_type IS NULL)
        OR
        (zone_direction IN ('EAST', 'WEST', 'SOUTH', 'NORTH') AND ticket_type IN ('VIP', 'NORMAL'))
    );

CREATE TABLE club_home_stadium_config (
    club_id BIGINT UNSIGNED NOT NULL COMMENT '俱乐部，每个俱乐部最多一个当前标准私有主场',
    stadium_id BIGINT UNSIGNED NOT NULL COMMENT '标准私有主场',
    rows_per_zone INT UNSIGNED NOT NULL COMMENT '每个VIP/NORMAL票区的排数',
    long_side_seats_per_row INT UNSIGNED NOT NULL COMMENT '东西长边每排座位数',
    short_side_seats_per_row INT UNSIGNED NOT NULL COMMENT '南北宽边每排座位数',
    vip_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT 'VIP默认票价',
    normal_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '普通默认票价',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (club_id),
    CONSTRAINT uq_club_home_stadium_config_stadium UNIQUE (stadium_id),
    CONSTRAINT fk_home_stadium_config_club FOREIGN KEY (club_id) REFERENCES club_info (club_id),
    CONSTRAINT fk_home_stadium_config_stadium FOREIGN KEY (stadium_id) REFERENCES stadium_info (stadium_id),
    CONSTRAINT ck_home_stadium_rows CHECK (rows_per_zone > 0),
    CONSTRAINT ck_home_stadium_long_side CHECK (long_side_seats_per_row > 0),
    CONSTRAINT ck_home_stadium_short_side CHECK (short_side_seats_per_row > 0),
    CONSTRAINT ck_home_stadium_vip_price CHECK (vip_price >= 0),
    CONSTRAINT ck_home_stadium_normal_price CHECK (normal_price >= 0)
) ENGINE=InnoDB COMMENT='俱乐部当前标准私有主场配置';
