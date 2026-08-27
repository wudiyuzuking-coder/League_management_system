-- 足球联赛购票系统：MySQL 8 正式数据库结构
-- 阶段2设计基线：静态场馆座位与单场比赛座位库存严格分离。

CREATE DATABASE IF NOT EXISTS league_ticket
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE league_ticket;

CREATE TABLE sys_role (
    role_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色主键',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '角色状态',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (role_id),
    CONSTRAINT uq_sys_role_code UNIQUE (role_code),
    CONSTRAINT ck_sys_role_status CHECK (role_status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB COMMENT='系统角色';

CREATE TABLE sys_permission (
    permission_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限主键',
    permission_code VARCHAR(64) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(80) NOT NULL COMMENT '权限名称',
    permission_status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '权限状态',
    description VARCHAR(255) NULL COMMENT '权限说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (permission_id),
    CONSTRAINT uq_sys_permission_code UNIQUE (permission_code),
    CONSTRAINT ck_sys_permission_status CHECK (permission_status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB COMMENT='系统权限';

CREATE TABLE sys_role_permission (
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色主键',
    permission_id BIGINT UNSIGNED NOT NULL COMMENT '权限主键',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (permission_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='角色权限关联';

CREATE TABLE sys_config (
    config_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '配置主键',
    config_key VARCHAR(64) NOT NULL COMMENT '配置键',
    config_value VARCHAR(255) NOT NULL COMMENT '配置值',
    value_type VARCHAR(16) NOT NULL DEFAULT 'STRING' COMMENT '值类型',
    description VARCHAR(255) NULL COMMENT '配置说明',
    config_status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '配置状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (config_id),
    CONSTRAINT uq_sys_config_key UNIQUE (config_key),
    CONSTRAINT ck_sys_config_type CHECK (value_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN')),
    CONSTRAINT ck_sys_config_status CHECK (config_status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB COMMENT='系统配置';

CREATE TABLE season_info (
    season_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '赛季主键',
    season_name VARCHAR(80) NOT NULL COMMENT '赛季名称',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    season_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '赛季状态',
    description VARCHAR(500) NULL COMMENT '赛季说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (season_id),
    CONSTRAINT uq_season_name UNIQUE (season_name),
    CONSTRAINT ck_season_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_season_status CHECK (season_status IN ('DRAFT', 'ACTIVE', 'FINISHED'))
) ENGINE=InnoDB COMMENT='联赛赛季';

CREATE TABLE round_info (
    round_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '轮次主键',
    season_id BIGINT UNSIGNED NOT NULL COMMENT '所属赛季',
    round_no INT UNSIGNED NOT NULL COMMENT '轮次编号',
    round_name VARCHAR(80) NOT NULL COMMENT '轮次名称',
    start_date DATE NULL COMMENT '轮次开始日期',
    end_date DATE NULL COMMENT '轮次结束日期',
    round_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '轮次状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (round_id),
    CONSTRAINT uq_round_season_no UNIQUE (season_id, round_no),
    CONSTRAINT uq_round_id_season UNIQUE (round_id, season_id),
    CONSTRAINT fk_round_season FOREIGN KEY (season_id) REFERENCES season_info (season_id),
    CONSTRAINT ck_round_no CHECK (round_no > 0),
    CONSTRAINT ck_round_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_round_status CHECK (round_status IN ('DRAFT', 'PUBLISHED', 'FINISHED'))
) ENGINE=InnoDB COMMENT='联赛轮次';

CREATE TABLE stadium_info (
    stadium_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场馆主键',
    stadium_name VARCHAR(100) NOT NULL COMMENT '场馆名称',
    city VARCHAR(50) NOT NULL COMMENT '所在城市',
    address VARCHAR(255) NOT NULL COMMENT '详细地址',
    capacity INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '标称容量',
    layout_description VARCHAR(500) NULL COMMENT '座位布局说明',
    stadium_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '场馆状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (stadium_id),
    CONSTRAINT uq_stadium_name_city UNIQUE (stadium_name, city),
    CONSTRAINT ck_stadium_capacity CHECK (capacity >= 0),
    CONSTRAINT ck_stadium_status CHECK (stadium_status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB COMMENT='场馆信息';

CREATE TABLE stadium_zone (
    stadium_zone_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场馆静态票区主键',
    stadium_id BIGINT UNSIGNED NOT NULL COMMENT '所属场馆',
    zone_code VARCHAR(32) NOT NULL COMMENT '场馆内票区编码',
    zone_name VARCHAR(80) NOT NULL COMMENT '票区名称',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '展示顺序',
    zone_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '静态票区状态',
    description VARCHAR(255) NULL COMMENT '票区说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (stadium_zone_id),
    CONSTRAINT uq_stadium_zone_code UNIQUE (stadium_id, zone_code),
    CONSTRAINT uq_stadium_zone_name UNIQUE (stadium_id, zone_name),
    CONSTRAINT uq_stadium_zone_id_stadium UNIQUE (stadium_zone_id, stadium_id),
    CONSTRAINT fk_stadium_zone_stadium FOREIGN KEY (stadium_id) REFERENCES stadium_info (stadium_id),
    KEY idx_stadium_zone_status (stadium_id, zone_status, sort_order)
) ENGINE=InnoDB COMMENT='场馆静态票区';

CREATE TABLE stadium_seat (
    stadium_seat_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '物理座位主键',
    stadium_id BIGINT UNSIGNED NOT NULL COMMENT '所属场馆',
    stadium_zone_id BIGINT UNSIGNED NOT NULL COMMENT '所属静态票区',
    row_no VARCHAR(20) NOT NULL COMMENT '显示排号',
    row_seq INT UNSIGNED NOT NULL COMMENT '排排序值，越小越靠前',
    seat_no VARCHAR(20) NOT NULL COMMENT '显示座号',
    seat_seq INT UNSIGNED NOT NULL COMMENT '同排座位排序值',
    center_distance DECIMAL(8,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT '距看台中线距离，越小越居中',
    seat_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '物理座位状态，仅ACTIVE或DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (stadium_seat_id),
    CONSTRAINT uq_stadium_seat_label UNIQUE (stadium_zone_id, row_no, seat_no),
    CONSTRAINT uq_stadium_seat_sequence UNIQUE (stadium_zone_id, row_seq, seat_seq),
    CONSTRAINT fk_stadium_seat_stadium FOREIGN KEY (stadium_id) REFERENCES stadium_info (stadium_id),
    CONSTRAINT fk_stadium_seat_zone FOREIGN KEY (stadium_zone_id, stadium_id) REFERENCES stadium_zone (stadium_zone_id, stadium_id),
    CONSTRAINT ck_stadium_seat_sequence CHECK (row_seq > 0 AND seat_seq > 0),
    CONSTRAINT ck_stadium_seat_status CHECK (seat_status IN ('ACTIVE', 'DISABLED')),
    KEY idx_stadium_seat_allocation (stadium_zone_id, seat_status, row_seq, seat_seq, center_distance)
) ENGINE=InnoDB COMMENT='场馆物理座位';

CREATE TABLE club_info (
    club_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '俱乐部主键',
    club_name VARCHAR(100) NOT NULL COMMENT '俱乐部名称',
    short_name VARCHAR(40) NULL COMMENT '俱乐部简称',
    logo_url VARCHAR(500) NULL COMMENT '队徽地址',
    home_city VARCHAR(50) NOT NULL COMMENT '主场城市',
    home_address VARCHAR(255) NULL COMMENT '主场地址说明',
    home_stadium_id BIGINT UNSIGNED NULL COMMENT '默认主场',
    description TEXT NULL COMMENT '俱乐部简介',
    club_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '俱乐部状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (club_id),
    CONSTRAINT uq_club_name UNIQUE (club_name),
    CONSTRAINT fk_club_home_stadium FOREIGN KEY (home_stadium_id) REFERENCES stadium_info (stadium_id) ON DELETE SET NULL,
    CONSTRAINT ck_club_status CHECK (club_status IN ('ACTIVE', 'DISABLED')),
    KEY idx_club_home_stadium (home_stadium_id)
) ENGINE=InnoDB COMMENT='俱乐部信息';

CREATE TABLE player_info (
    player_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '球员主键',
    club_id BIGINT UNSIGNED NOT NULL COMMENT '所属俱乐部',
    player_name VARCHAR(80) NOT NULL COMMENT '球员姓名',
    shirt_no INT UNSIGNED NULL COMMENT '球衣号码',
    position VARCHAR(20) NOT NULL COMMENT '场上位置',
    nationality VARCHAR(50) NULL COMMENT '国籍',
    birth_date DATE NULL COMMENT '出生日期',
    player_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '球员状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (player_id),
    CONSTRAINT uq_player_club_shirt UNIQUE (club_id, shirt_no),
    CONSTRAINT fk_player_club FOREIGN KEY (club_id) REFERENCES club_info (club_id),
    CONSTRAINT ck_player_shirt_no CHECK (shirt_no IS NULL OR shirt_no BETWEEN 1 AND 99),
    CONSTRAINT ck_player_position CHECK (position IN ('GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD')),
    CONSTRAINT ck_player_status CHECK (player_status IN ('ACTIVE', 'INACTIVE', 'TRANSFERRED')),
    KEY idx_player_club_status (club_id, player_status)
) ENGINE=InnoDB COMMENT='球员信息';

CREATE TABLE player_season_stat (
    stat_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '球员赛季统计主键',
    season_id BIGINT UNSIGNED NOT NULL COMMENT '赛季',
    player_id BIGINT UNSIGNED NOT NULL COMMENT '球员',
    club_id BIGINT UNSIGNED NOT NULL COMMENT '统计归属俱乐部',
    appearances INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '出场次数',
    starts INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '首发次数',
    goals INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '进球数',
    assists INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '助攻数',
    yellow_cards INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '黄牌数',
    red_cards INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '红牌数',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (stat_id),
    CONSTRAINT uq_player_season_club UNIQUE (season_id, player_id, club_id),
    CONSTRAINT fk_player_stat_season FOREIGN KEY (season_id) REFERENCES season_info (season_id),
    CONSTRAINT fk_player_stat_player FOREIGN KEY (player_id) REFERENCES player_info (player_id),
    CONSTRAINT fk_player_stat_club FOREIGN KEY (club_id) REFERENCES club_info (club_id),
    CONSTRAINT ck_player_stat_starts CHECK (starts <= appearances),
    KEY idx_player_stat_ranking (season_id, goals, assists)
) ENGINE=InnoDB COMMENT='球员赛季统计';

CREATE TABLE coach_info (
    coach_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教练主键',
    club_id BIGINT UNSIGNED NOT NULL COMMENT '所属俱乐部',
    coach_name VARCHAR(80) NOT NULL COMMENT '教练姓名',
    title VARCHAR(50) NOT NULL COMMENT '职务',
    nationality VARCHAR(50) NULL COMMENT '国籍',
    description VARCHAR(500) NULL COMMENT '简介',
    coach_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '教练状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (coach_id),
    CONSTRAINT fk_coach_club FOREIGN KEY (club_id) REFERENCES club_info (club_id),
    CONSTRAINT ck_coach_status CHECK (coach_status IN ('ACTIVE', 'INACTIVE')),
    KEY idx_coach_club_status (club_id, coach_status)
) ENGINE=InnoDB COMMENT='教练信息';

CREATE TABLE club_season_record (
    record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '俱乐部赛季战绩主键',
    season_id BIGINT UNSIGNED NOT NULL COMMENT '赛季',
    club_id BIGINT UNSIGNED NOT NULL COMMENT '俱乐部',
    played INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '比赛场次',
    wins INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '胜场',
    draws INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '平场',
    losses INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '负场',
    goals_for INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '进球',
    goals_against INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '失球',
    points INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '积分',
    ranking INT UNSIGNED NULL COMMENT '排名',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (record_id),
    CONSTRAINT uq_club_season UNIQUE (season_id, club_id),
    CONSTRAINT fk_club_record_season FOREIGN KEY (season_id) REFERENCES season_info (season_id),
    CONSTRAINT fk_club_record_club FOREIGN KEY (club_id) REFERENCES club_info (club_id),
    CONSTRAINT ck_club_record_played CHECK (played = wins + draws + losses),
    CONSTRAINT ck_club_record_ranking CHECK (ranking IS NULL OR ranking > 0),
    KEY idx_club_record_ranking (season_id, points, ranking)
) ENGINE=InnoDB COMMENT='俱乐部赛季战绩';

CREATE TABLE sys_user (
    user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    username VARCHAR(50) NOT NULL COMMENT '登录用户名',
    phone VARCHAR(20) NULL COMMENT '手机号',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希；阶段2演示数据为不可登录标记',
    display_name VARCHAR(80) NOT NULL COMMENT '显示名称',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色',
    club_id BIGINT UNSIGNED NULL COMMENT '俱乐部账号或检票员所属俱乐部',
    user_status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态',
    last_login_at DATETIME NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (user_id),
    CONSTRAINT uq_sys_user_username UNIQUE (username),
    CONSTRAINT uq_sys_user_phone UNIQUE (phone),
    CONSTRAINT fk_sys_user_role FOREIGN KEY (role_id) REFERENCES sys_role (role_id),
    CONSTRAINT fk_sys_user_club FOREIGN KEY (club_id) REFERENCES club_info (club_id) ON DELETE SET NULL,
    CONSTRAINT ck_sys_user_status CHECK (user_status IN ('ENABLED', 'DISABLED', 'LOCKED')),
    KEY idx_sys_user_role_status (role_id, user_status),
    KEY idx_sys_user_club (club_id)
) ENGINE=InnoDB COMMENT='系统用户与后台账号';

CREATE TABLE operation_log (
    log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '操作日志主键',
    operator_id BIGINT UNSIGNED NULL COMMENT '操作用户，匿名操作可为空',
    module_name VARCHAR(50) NOT NULL COMMENT '业务模块',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型',
    request_method VARCHAR(10) NULL COMMENT 'HTTP方法',
    request_uri VARCHAR(255) NULL COMMENT '请求地址',
    operation_description VARCHAR(500) NULL COMMENT '操作说明',
    ip_address VARCHAR(64) NULL COMMENT '客户端IP',
    result_status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果',
    error_message VARCHAR(1000) NULL COMMENT '失败信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (log_id),
    CONSTRAINT fk_operation_log_user FOREIGN KEY (operator_id) REFERENCES sys_user (user_id) ON DELETE SET NULL,
    CONSTRAINT ck_operation_log_result CHECK (result_status IN ('SUCCESS', 'FAILURE')),
    KEY idx_operation_log_operator_time (operator_id, created_at),
    KEY idx_operation_log_module_time (module_name, created_at)
) ENGINE=InnoDB COMMENT='后台操作日志';

CREATE TABLE match_info (
    match_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '比赛主键',
    season_id BIGINT UNSIGNED NOT NULL COMMENT '赛季',
    round_id BIGINT UNSIGNED NOT NULL COMMENT '轮次',
    home_club_id BIGINT UNSIGNED NOT NULL COMMENT '主队俱乐部',
    away_club_id BIGINT UNSIGNED NOT NULL COMMENT '客队俱乐部',
    stadium_id BIGINT UNSIGNED NOT NULL COMMENT '比赛场馆',
    match_time DATETIME NOT NULL COMMENT '开赛时间',
    sale_start_time DATETIME NULL COMMENT '全场售票开始时间',
    sale_end_time DATETIME NULL COMMENT '全场售票停止时间',
    home_score INT UNSIGNED NULL COMMENT '主队比分',
    away_score INT UNSIGNED NULL COMMENT '客队比分',
    match_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '比赛状态',
    published_at DATETIME NULL COMMENT '首次发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (match_id),
    CONSTRAINT fk_match_season FOREIGN KEY (season_id) REFERENCES season_info (season_id),
    CONSTRAINT fk_match_round FOREIGN KEY (round_id, season_id) REFERENCES round_info (round_id, season_id),
    CONSTRAINT fk_match_home_club FOREIGN KEY (home_club_id) REFERENCES club_info (club_id),
    CONSTRAINT fk_match_away_club FOREIGN KEY (away_club_id) REFERENCES club_info (club_id),
    CONSTRAINT fk_match_stadium FOREIGN KEY (stadium_id) REFERENCES stadium_info (stadium_id),
    CONSTRAINT ck_match_clubs CHECK (home_club_id <> away_club_id),
    CONSTRAINT ck_match_sale_time CHECK (sale_end_time IS NULL OR sale_start_time IS NULL OR sale_end_time >= sale_start_time),
    CONSTRAINT ck_match_score_pair CHECK ((home_score IS NULL AND away_score IS NULL) OR (home_score IS NOT NULL AND away_score IS NOT NULL)),
    CONSTRAINT ck_match_status CHECK (match_status IN ('DRAFT', 'PUBLISHED', 'IN_PROGRESS', 'FINISHED', 'CANCELLED')),
    KEY idx_match_season_round (season_id, round_id),
    KEY idx_match_status_time (match_status, match_time),
    KEY idx_match_home_time (home_club_id, match_time),
    KEY idx_match_away_time (away_club_id, match_time)
) ENGINE=InnoDB COMMENT='比赛信息；不对赛季主客队组合设置唯一约束';

CREATE TABLE match_ticket_zone (
    match_zone_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '单场比赛票区主键',
    match_id BIGINT UNSIGNED NOT NULL COMMENT '比赛',
    stadium_zone_id BIGINT UNSIGNED NOT NULL COMMENT '对应场馆静态票区',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建管理员',
    zone_name_snapshot VARCHAR(80) NOT NULL COMMENT '售票时票区名称快照',
    ticket_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '单价',
    zone_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '比赛票区状态',
    sale_start_time DATETIME NULL COMMENT '票区售票开始时间',
    sale_end_time DATETIME NULL COMMENT '票区售票停止时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (match_zone_id),
    CONSTRAINT uq_match_ticket_zone UNIQUE (match_id, stadium_zone_id),
    CONSTRAINT uq_match_zone_id_match UNIQUE (match_zone_id, match_id),
    CONSTRAINT fk_match_zone_match FOREIGN KEY (match_id) REFERENCES match_info (match_id),
    CONSTRAINT fk_match_zone_stadium_zone FOREIGN KEY (stadium_zone_id) REFERENCES stadium_zone (stadium_zone_id),
    CONSTRAINT fk_match_zone_created_by FOREIGN KEY (created_by) REFERENCES sys_user (user_id),
    CONSTRAINT ck_match_zone_price CHECK (ticket_price >= 0),
    CONSTRAINT ck_match_zone_sale_time CHECK (sale_end_time IS NULL OR sale_start_time IS NULL OR sale_end_time >= sale_start_time),
    CONSTRAINT ck_match_zone_status CHECK (zone_status IN ('DRAFT', 'ON_SALE', 'PAUSED', 'CLOSED')),
    KEY idx_match_zone_query (match_id, zone_status)
) ENGINE=InnoDB COMMENT='单场比赛票区配置';

CREATE TABLE match_seat_inventory (
    inventory_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '单场比赛座位库存主键',
    match_id BIGINT UNSIGNED NOT NULL COMMENT '比赛',
    match_zone_id BIGINT UNSIGNED NOT NULL COMMENT '比赛票区',
    stadium_seat_id BIGINT UNSIGNED NOT NULL COMMENT '物理座位',
    inventory_status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE' COMMENT '比赛座位状态',
    lock_order_id BIGINT UNSIGNED NULL COMMENT '当前锁定该座位的订单',
    locked_at DATETIME NULL COMMENT '锁定时间',
    lock_expire_time DATETIME NULL COMMENT '锁定到期时间',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (inventory_id),
    CONSTRAINT uq_match_seat_inventory UNIQUE (match_id, stadium_seat_id),
    CONSTRAINT fk_inventory_match FOREIGN KEY (match_id) REFERENCES match_info (match_id),
    CONSTRAINT fk_inventory_match_zone FOREIGN KEY (match_zone_id, match_id) REFERENCES match_ticket_zone (match_zone_id, match_id),
    CONSTRAINT fk_inventory_stadium_seat FOREIGN KEY (stadium_seat_id) REFERENCES stadium_seat (stadium_seat_id),
    CONSTRAINT ck_inventory_status CHECK (inventory_status IN ('AVAILABLE', 'LOCKED', 'SOLD', 'DISABLED')),
    CONSTRAINT ck_inventory_lock_fields CHECK (
        (inventory_status = 'LOCKED' AND lock_order_id IS NOT NULL AND locked_at IS NOT NULL AND lock_expire_time IS NOT NULL AND lock_expire_time > locked_at)
        OR
        (inventory_status <> 'LOCKED' AND lock_order_id IS NULL AND locked_at IS NULL AND lock_expire_time IS NULL)
    ),
    KEY idx_inventory_available (match_zone_id, inventory_status, stadium_seat_id),
    KEY idx_inventory_lock_expire (inventory_status, lock_expire_time),
    KEY idx_inventory_lock_order (lock_order_id)
) ENGINE=InnoDB COMMENT='单场比赛座位库存';

CREATE TABLE ticket_order (
    order_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单主键',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '下单用户',
    match_id BIGINT UNSIGNED NOT NULL COMMENT '比赛',
    match_zone_id BIGINT UNSIGNED NOT NULL COMMENT '比赛票区，一单仅限一个票区',
    ticket_count INT UNSIGNED NOT NULL COMMENT '购票张数，最多4张',
    total_amount DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '订单总金额',
    order_status VARCHAR(24) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态',
    expire_time DATETIME NOT NULL COMMENT '待支付到期时间',
    paid_at DATETIME NULL COMMENT '支付成功时间',
    cancelled_at DATETIME NULL COMMENT '取消时间',
    cancel_reason VARCHAR(32) NULL COMMENT '取消原因',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    remark VARCHAR(255) NULL COMMENT '订单备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (order_id),
    CONSTRAINT uq_ticket_order_no UNIQUE (order_no),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user (user_id),
    CONSTRAINT fk_order_match FOREIGN KEY (match_id) REFERENCES match_info (match_id),
    CONSTRAINT fk_order_match_zone FOREIGN KEY (match_zone_id, match_id) REFERENCES match_ticket_zone (match_zone_id, match_id),
    CONSTRAINT ck_order_ticket_count CHECK (ticket_count BETWEEN 1 AND 4),
    CONSTRAINT ck_order_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_order_status CHECK (order_status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'REFUND_PENDING', 'REFUNDED')),
    KEY idx_order_user_time (user_id, created_at),
    KEY idx_order_match_status (match_id, order_status),
    KEY idx_order_payment_expire (order_status, expire_time)
) ENGINE=InnoDB COMMENT='票务订单；一单只对应一场比赛和一个比赛票区';

ALTER TABLE match_seat_inventory
    ADD CONSTRAINT fk_inventory_lock_order FOREIGN KEY (lock_order_id) REFERENCES ticket_order (order_id);

CREATE TABLE order_item (
    item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单明细主键',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单',
    inventory_id BIGINT UNSIGNED NOT NULL COMMENT '所绑定的单场比赛座位库存',
    ticket_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '成交单价快照',
    zone_name_snapshot VARCHAR(80) NOT NULL COMMENT '票区名称快照',
    row_no_snapshot VARCHAR(20) NOT NULL COMMENT '排号快照',
    seat_no_snapshot VARCHAR(20) NOT NULL COMMENT '座号快照',
    item_status VARCHAR(16) NOT NULL DEFAULT 'LOCKED' COMMENT '订单明细状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (item_id),
    CONSTRAINT uq_order_item_inventory UNIQUE (order_id, inventory_id),
    CONSTRAINT uq_order_item_id_order UNIQUE (item_id, order_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES ticket_order (order_id),
    CONSTRAINT fk_order_item_inventory FOREIGN KEY (inventory_id) REFERENCES match_seat_inventory (inventory_id),
    CONSTRAINT ck_order_item_price CHECK (ticket_price >= 0),
    CONSTRAINT ck_order_item_status CHECK (item_status IN ('LOCKED', 'PAID', 'CANCELLED', 'REFUNDED')),
    KEY idx_order_item_inventory (inventory_id)
) ENGINE=InnoDB COMMENT='订单座位明细；每条明细绑定一个比赛座位库存';

CREATE TABLE payment_record (
    payment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付记录主键',
    payment_no VARCHAR(32) NOT NULL COMMENT '支付流水号',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单',
    pay_amount DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '支付金额',
    pay_method VARCHAR(20) NOT NULL DEFAULT 'SIMULATED' COMMENT '支付方式',
    pay_status VARCHAR(16) NOT NULL DEFAULT 'CREATED' COMMENT '支付状态',
    third_party_trade_no VARCHAR(64) NULL COMMENT '模拟或第三方交易号',
    pay_time DATETIME NULL COMMENT '支付成功时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (payment_id),
    CONSTRAINT uq_payment_no UNIQUE (payment_no),
    CONSTRAINT uq_payment_trade_no UNIQUE (third_party_trade_no),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES ticket_order (order_id),
    CONSTRAINT ck_payment_amount CHECK (pay_amount >= 0),
    CONSTRAINT ck_payment_method CHECK (pay_method IN ('SIMULATED')),
    CONSTRAINT ck_payment_status CHECK (pay_status IN ('CREATED', 'SUCCESS', 'FAILED', 'CLOSED')),
    KEY idx_payment_order_status (order_id, pay_status),
    KEY idx_payment_status_time (pay_status, created_at)
) ENGINE=InnoDB COMMENT='模拟支付记录';

CREATE TABLE e_ticket (
    ticket_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '电子票主键',
    ticket_code VARCHAR(64) NOT NULL COMMENT '全局唯一票码',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单',
    item_id BIGINT UNSIGNED NOT NULL COMMENT '订单明细，每个明细最多一张电子票',
    ticket_status VARCHAR(16) NOT NULL DEFAULT 'UNUSED' COMMENT '电子票状态',
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '出票时间',
    used_at DATETIME NULL COMMENT '成功入场时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (ticket_id),
    CONSTRAINT uq_e_ticket_code UNIQUE (ticket_code),
    CONSTRAINT uq_e_ticket_item UNIQUE (item_id),
    CONSTRAINT fk_e_ticket_order FOREIGN KEY (order_id) REFERENCES ticket_order (order_id),
    CONSTRAINT fk_e_ticket_item FOREIGN KEY (item_id, order_id) REFERENCES order_item (item_id, order_id),
    CONSTRAINT ck_e_ticket_status CHECK (ticket_status IN ('UNUSED', 'USED', 'REFUNDED', 'VOID')),
    CONSTRAINT ck_e_ticket_used_time CHECK ((ticket_status = 'USED' AND used_at IS NOT NULL) OR (ticket_status <> 'USED' AND used_at IS NULL)),
    KEY idx_e_ticket_order_status (order_id, ticket_status)
) ENGINE=InnoDB COMMENT='电子票；座位通过订单明细关联至比赛库存';

CREATE TABLE refund_apply (
    refund_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退票申请主键',
    refund_no VARCHAR(32) NOT NULL COMMENT '退票申请号',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '整单退票的订单',
    applicant_id BIGINT UNSIGNED NOT NULL COMMENT '申请用户',
    reason VARCHAR(500) NOT NULL COMMENT '退票原因',
    refund_amount DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '申请退款金额',
    refund_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
    auditor_id BIGINT UNSIGNED NULL COMMENT '审核管理员',
    audit_remark VARCHAR(500) NULL COMMENT '审核意见',
    audit_time DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (refund_id),
    CONSTRAINT uq_refund_no UNIQUE (refund_no),
    CONSTRAINT uq_refund_order UNIQUE (order_id),
    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES ticket_order (order_id),
    CONSTRAINT fk_refund_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user (user_id),
    CONSTRAINT fk_refund_auditor FOREIGN KEY (auditor_id) REFERENCES sys_user (user_id),
    CONSTRAINT ck_refund_amount CHECK (refund_amount >= 0),
    CONSTRAINT ck_refund_status CHECK (refund_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_refund_audit_fields CHECK (
        (refund_status = 'PENDING' AND auditor_id IS NULL AND audit_time IS NULL)
        OR
        (refund_status IN ('APPROVED', 'REJECTED') AND auditor_id IS NOT NULL AND audit_time IS NOT NULL)
    ),
    KEY idx_refund_status_time (refund_status, created_at),
    KEY idx_refund_applicant_time (applicant_id, created_at)
) ENGINE=InnoDB COMMENT='整单退票申请';

CREATE TABLE checkin_record (
    checkin_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '检票记录主键',
    match_id BIGINT UNSIGNED NOT NULL COMMENT '检票场次',
    ticket_id BIGINT UNSIGNED NULL COMMENT '识别出的电子票，无效票码时可为空',
    scanned_ticket_code VARCHAR(64) NOT NULL COMMENT '本次扫描的票码原文',
    checker_id BIGINT UNSIGNED NOT NULL COMMENT '检票员',
    check_result VARCHAR(24) NOT NULL COMMENT '检票结果',
    check_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检票时间',
    remark VARCHAR(255) NULL COMMENT '结果说明',
    PRIMARY KEY (checkin_id),
    CONSTRAINT fk_checkin_match FOREIGN KEY (match_id) REFERENCES match_info (match_id),
    CONSTRAINT fk_checkin_ticket FOREIGN KEY (ticket_id) REFERENCES e_ticket (ticket_id),
    CONSTRAINT fk_checkin_checker FOREIGN KEY (checker_id) REFERENCES sys_user (user_id),
    CONSTRAINT ck_checkin_result CHECK (check_result IN ('SUCCESS', 'CODE_NOT_FOUND', 'WRONG_MATCH', 'ORDER_INVALID', 'TICKET_USED', 'TICKET_REFUNDED', 'TICKET_VOID')),
    KEY idx_checkin_ticket_time (ticket_id, check_time),
    KEY idx_checkin_match_time (match_id, check_time),
    KEY idx_checkin_checker_time (checker_id, check_time),
    KEY idx_checkin_scanned_code (scanned_ticket_code)
) ENGINE=InnoDB COMMENT='电子票检票尝试记录';
