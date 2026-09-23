-- 足球联赛购票系统：正式初始化数据
-- 维护固定角色、权限、系统参数，以及课程交付所需的根管理员和四支完整初始球队。
-- 不包含普通用户、赛事管理员、赛季、比赛、票务、订单或支付演示数据。

USE league_ticket;

INSERT INTO sys_role (role_code, role_name, role_status, remark)
VALUES
    ('USER', '普通用户', 'ENABLED', '浏览比赛、购票及管理本人订单'),
    ('CLUB', '俱乐部负责人', 'ENABLED', '维护本俱乐部资料、球员和教练'),
    ('EVENT_ADMIN', '赛事管理员', 'ENABLED', '管理赛季、比赛结果和票务配置'),
    ('ADMIN', '系统管理员', 'ENABLED', '管理用户、俱乐部注册审核和系统基础数据')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    role_status = VALUES(role_status),
    remark = VALUES(remark);

UPDATE sys_role
SET role_status = 'DISABLED',
    remark = '角色已移除，保留历史数据关联'
WHERE role_code = 'CHECKER';

INSERT INTO sys_permission (permission_code, permission_name, permission_status, description)
VALUES
    ('MATCH_VIEW', '查看比赛', 'ENABLED', '查看赛季、赛程和比赛票务信息'),
    ('ORDER_CREATE', '创建订单', 'ENABLED', '选择比赛票区并创建购票订单'),
    ('ORDER_VIEW_SELF', '查看本人订单', 'ENABLED', '查看当前用户自己的订单与电子票'),
    ('REFUND_APPLY', '申请退票', 'ENABLED', '针对本人符合条件的订单申请整单退票'),
    ('CLUB_MANAGE_SELF', '管理本俱乐部', 'ENABLED', '维护绑定俱乐部的资料、球员和教练'),
    ('CHECKIN', '现场检票', 'ENABLED', '核验指定场次电子票'),
    ('MATCH_MANAGE', '管理比赛', 'ENABLED', '管理赛季、轮次、赛程和比赛状态'),
    ('TICKET_MANAGE', '管理票务', 'ENABLED', '管理场馆座位、比赛票区和比赛座位库存'),
    ('REFUND_AUDIT', '审核退票（已停用）', 'DISABLED', '历史权限保留；正式退款流程已改为自动处理'),
    ('STATISTICS_VIEW', '查看统计', 'ENABLED', '查看上座率、销售额等统计数据'),
    ('USER_MANAGE', '管理用户', 'ENABLED', '管理用户、角色和账号状态')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_status = VALUES(permission_status),
    description = VALUES(description);

DELETE rp
FROM sys_role_permission rp
JOIN sys_role r ON r.role_id = rp.role_id
WHERE r.role_code IN ('CHECKER', 'EVENT_ADMIN', 'ADMIN');

-- 普通用户权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('MATCH_VIEW', 'ORDER_CREATE', 'ORDER_VIEW_SELF', 'REFUND_APPLY')
WHERE r.role_code = 'USER';

-- 俱乐部负责人权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('MATCH_VIEW', 'CLUB_MANAGE_SELF', 'STATISTICS_VIEW')
WHERE r.role_code = 'CLUB';

-- 赛事管理员权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('MATCH_VIEW', 'MATCH_MANAGE', 'TICKET_MANAGE', 'STATISTICS_VIEW')
WHERE r.role_code = 'EVENT_ADMIN';

-- 系统管理员权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('USER_MANAGE', 'CLUB_MANAGE_SELF')
WHERE r.role_code = 'ADMIN';

INSERT INTO sys_config (config_key, config_value, value_type, description, config_status)
VALUES
    ('ORDER_PAYMENT_TIMEOUT_MINUTES', '15', 'INTEGER', '待支付订单和锁座的超时时长（分钟）', 'ENABLED'),
    ('SALE_STOP_BEFORE_MINUTES', '60', 'INTEGER', '兼容配置；正式停售时间固定为比赛开始前60分钟', 'ENABLED'),
    ('REFUND_STOP_BEFORE_HOURS', '24', 'INTEGER', '比赛开始前停止申请退票的时间（小时）', 'ENABLED'),
    ('MAX_TICKETS_PER_ORDER', '4', 'INTEGER', '单笔订单最大购票张数', 'ENABLED'),
    ('SYSTEM_TIME_OFFSET_SECONDS', '0', 'INTEGER', '课程演示系统时间相对服务器真实时间的偏移秒数', 'ENABLED'),
    ('AUTO_SCHEDULE_DEFAULT_KICKOFF_TIME', '20:00', 'STRING', '兼容配置；正式自动排赛统一20:00开球', 'ENABLED')
ON DUPLICATE KEY UPDATE
    config_value = IF(config_key = 'SYSTEM_TIME_OFFSET_SECONDS', config_value, VALUES(config_value)),
    value_type = VALUES(value_type),
    description = VALUES(description),
    config_status = VALUES(config_status);

-- 正式根管理员。数据库仅保存 BCrypt 哈希，不保存明文口令。
SET @role_admin := (SELECT role_id FROM sys_role WHERE role_code = 'ADMIN');

INSERT INTO sys_user
    (username, phone, password_hash, display_name, employee_no, role_id, club_id, user_status)
VALUES
    ('root_admin', '13800000000', '$2a$10$ZkjnDmt57WwzbNfd36qDUOwgTqFUmbaX8z8gWudQZNCnYgg4a4F7C',
     '根管理员', 'SA0001', @role_admin, NULL, 'ENABLED')
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    phone = VALUES(phone),
    display_name = VALUES(display_name),
    employee_no = VALUES(employee_no),
    role_id = VALUES(role_id),
    club_id = NULL,
    user_status = 'ENABLED';

-- 四支正式初始俱乐部。
INSERT INTO club_info
    (club_name, short_name, home_city, description, club_status)
VALUES
    ('曼城足球俱乐部', '曼城', '曼彻斯特', '课程设计正式初始化俱乐部。', 'ACTIVE'),
    ('拜仁慕尼黑足球俱乐部', '拜仁慕尼黑', '慕尼黑', '课程设计正式初始化俱乐部。', 'ACTIVE'),
    ('巴塞罗那足球俱乐部', '巴塞罗那', '巴塞罗那', '课程设计正式初始化俱乐部。', 'ACTIVE'),
    ('迈阿密国际足球俱乐部', '迈阿密国际', '迈阿密', '课程设计正式初始化俱乐部。', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    short_name = VALUES(short_name),
    home_city = VALUES(home_city),
    description = VALUES(description),
    club_status = 'ACTIVE';

-- 四个已启用的CLUB负责人账号，与四支俱乐部一一绑定。
-- 初始密码与根管理员相同，数据库中仅保存BCrypt哈希；重复执行seed不会重置已修改密码。
SET @role_club := (SELECT role_id FROM sys_role WHERE role_code = 'CLUB');

INSERT INTO sys_user
    (username, phone, password_hash, display_name, club_apply_name, employee_no, role_id, club_id, user_status)
SELECT account.username,
       account.phone,
       '$2a$10$ZkjnDmt57WwzbNfd36qDUOwgTqFUmbaX8z8gWudQZNCnYgg4a4F7C',
       account.display_name,
       c.club_name,
       NULL,
       @role_club,
       c.club_id,
       'ENABLED'
FROM (
    SELECT 'club_manchester_city' username, '13800000001' phone, '曼城俱乐部负责人' display_name, '曼城足球俱乐部' club_name UNION ALL
    SELECT 'club_bayern_munich', '13800000002', '拜仁慕尼黑俱乐部负责人', '拜仁慕尼黑足球俱乐部' UNION ALL
    SELECT 'club_barcelona', '13800000003', '巴塞罗那俱乐部负责人', '巴塞罗那足球俱乐部' UNION ALL
    SELECT 'club_inter_miami', '13800000004', '迈阿密国际俱乐部负责人', '迈阿密国际足球俱乐部'
) account
JOIN club_info c ON c.club_name = account.club_name
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    phone = VALUES(phone),
    display_name = VALUES(display_name),
    club_apply_name = VALUES(club_apply_name),
    employee_no = NULL,
    role_id = VALUES(role_id),
    club_id = VALUES(club_id),
    user_status = 'ENABLED';

-- 每队一个完整STANDARD_8私有主场：8个静态票区、每区3排×8座，共192座。
INSERT INTO stadium_info
    (stadium_name, city, address, capacity, layout_description, venue_model, stadium_status)
VALUES
    ('曼城标准主场', '曼彻斯特', '曼彻斯特体育大道1号', 192, 'STANDARD_8: rows=3, long=8, short=8', 'STANDARD_8', 'ACTIVE'),
    ('拜仁慕尼黑标准主场', '慕尼黑', '慕尼黑体育大道1号', 192, 'STANDARD_8: rows=3, long=8, short=8', 'STANDARD_8', 'ACTIVE'),
    ('巴塞罗那标准主场', '巴塞罗那', '巴塞罗那体育大道1号', 192, 'STANDARD_8: rows=3, long=8, short=8', 'STANDARD_8', 'ACTIVE'),
    ('迈阿密国际标准主场', '迈阿密', '迈阿密体育大道1号', 192, 'STANDARD_8: rows=3, long=8, short=8', 'STANDARD_8', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    address = VALUES(address),
    capacity = VALUES(capacity),
    layout_description = VALUES(layout_description),
    venue_model = 'STANDARD_8',
    stadium_status = 'ACTIVE';

UPDATE club_info c
JOIN (
    SELECT '曼城足球俱乐部' club_name, '曼城标准主场' stadium_name, '曼彻斯特' city, '曼彻斯特体育大道1号' address UNION ALL
    SELECT '拜仁慕尼黑足球俱乐部', '拜仁慕尼黑标准主场', '慕尼黑', '慕尼黑体育大道1号' UNION ALL
    SELECT '巴塞罗那足球俱乐部', '巴塞罗那标准主场', '巴塞罗那', '巴塞罗那体育大道1号' UNION ALL
    SELECT '迈阿密国际足球俱乐部', '迈阿密国际标准主场', '迈阿密', '迈阿密体育大道1号'
) mapping ON mapping.club_name = c.club_name
JOIN stadium_info s ON s.stadium_name = mapping.stadium_name AND s.city = mapping.city
SET c.home_city = mapping.city,
    c.home_address = mapping.address,
    c.home_stadium_id = s.stadium_id;

INSERT INTO club_home_stadium_config
    (club_id, stadium_id, rows_per_zone, long_side_seats_per_row, short_side_seats_per_row, vip_price, normal_price)
SELECT c.club_id, c.home_stadium_id, 3, 8, 8, 200.00, 100.00
FROM club_info c
WHERE c.club_name IN (
    '曼城足球俱乐部',
    '拜仁慕尼黑足球俱乐部',
    '巴塞罗那足球俱乐部',
    '迈阿密国际足球俱乐部'
)
ON DUPLICATE KEY UPDATE
    stadium_id = VALUES(stadium_id),
    rows_per_zone = VALUES(rows_per_zone),
    long_side_seats_per_row = VALUES(long_side_seats_per_row),
    short_side_seats_per_row = VALUES(short_side_seats_per_row),
    vip_price = VALUES(vip_price),
    normal_price = VALUES(normal_price);

INSERT INTO stadium_zone
    (stadium_id, zone_code, zone_name, zone_direction, ticket_type, sort_order, zone_status, description)
SELECT s.stadium_id,
       zone.zone_code,
       zone.zone_name,
       zone.zone_direction,
       zone.ticket_type,
       zone.sort_order,
       'ACTIVE',
       '标准私有主场票区'
FROM stadium_info s
CROSS JOIN (
    SELECT 'EAST_VIP' zone_code, '东 VIP' zone_name, 'EAST' zone_direction, 'VIP' ticket_type, 1 sort_order UNION ALL
    SELECT 'EAST_NORMAL', '东 普通', 'EAST', 'NORMAL', 2 UNION ALL
    SELECT 'WEST_VIP', '西 VIP', 'WEST', 'VIP', 3 UNION ALL
    SELECT 'WEST_NORMAL', '西 普通', 'WEST', 'NORMAL', 4 UNION ALL
    SELECT 'SOUTH_VIP', '南 VIP', 'SOUTH', 'VIP', 5 UNION ALL
    SELECT 'SOUTH_NORMAL', '南 普通', 'SOUTH', 'NORMAL', 6 UNION ALL
    SELECT 'NORTH_VIP', '北 VIP', 'NORTH', 'VIP', 7 UNION ALL
    SELECT 'NORTH_NORMAL', '北 普通', 'NORTH', 'NORMAL', 8
) zone
WHERE s.stadium_name IN ('曼城标准主场', '拜仁慕尼黑标准主场', '巴塞罗那标准主场', '迈阿密国际标准主场')
ON DUPLICATE KEY UPDATE
    zone_name = VALUES(zone_name),
    zone_direction = VALUES(zone_direction),
    ticket_type = VALUES(ticket_type),
    sort_order = VALUES(sort_order),
    zone_status = 'ACTIVE',
    description = VALUES(description);

INSERT INTO stadium_seat
    (stadium_id, stadium_zone_id, row_no, row_seq, seat_no, seat_seq, center_distance, seat_status)
SELECT z.stadium_id,
       z.stadium_zone_id,
       CONCAT(CASE WHEN z.ticket_type = 'NORMAL' THEN row_no.n + cfg.rows_per_zone ELSE row_no.n END, '排'),
       CASE WHEN z.ticket_type = 'NORMAL' THEN row_no.n + cfg.rows_per_zone ELSE row_no.n END,
       CONCAT(seat_no.n, '座'),
       seat_no.n,
       ABS(seat_no.n - 4.5),
       'ACTIVE'
FROM stadium_zone z
JOIN club_home_stadium_config cfg ON cfg.stadium_id = z.stadium_id
CROSS JOIN (
    SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3
) row_no
CROSS JOIN (
    SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) seat_no
WHERE z.zone_status = 'ACTIVE'
  AND z.stadium_id IN (
      SELECT home_stadium_id
      FROM club_info
      WHERE club_name IN ('曼城足球俱乐部', '拜仁慕尼黑足球俱乐部', '巴塞罗那足球俱乐部', '迈阿密国际足球俱乐部')
  )
ON DUPLICATE KEY UPDATE
    row_no = VALUES(row_no),
    seat_no = VALUES(seat_no),
    center_distance = VALUES(center_distance),
    seat_status = 'ACTIVE';

-- 每队18名现役球员：11名首发（1门将、4后卫、3中场、3前锋）和7名替补。
-- 出生年份相对数据库当前年份生成，初始化时年龄为20至31岁，满足18至50岁规则。
INSERT INTO player_info
    (club_id, player_name, shirt_no, position, nationality, birth_date, birth_year, player_status, lineup_role)
SELECT c.club_id,
       CONCAT(c.short_name, '球员', LPAD(r.shirt_no, 2, '0')),
       r.shirt_no,
       r.position,
       c.nationality,
       NULL,
       YEAR(CURRENT_DATE) - (20 + MOD(r.shirt_no, 12)),
       'ACTIVE',
       r.lineup_role
FROM (
    SELECT club_id, short_name,
           CASE club_name
               WHEN '曼城足球俱乐部' THEN '英国'
               WHEN '拜仁慕尼黑足球俱乐部' THEN '德国'
               WHEN '巴塞罗那足球俱乐部' THEN '西班牙'
               ELSE '美国'
           END AS nationality
    FROM club_info
    WHERE club_name IN (
        '曼城足球俱乐部',
        '拜仁慕尼黑足球俱乐部',
        '巴塞罗那足球俱乐部',
        '迈阿密国际足球俱乐部'
    )
) c
CROSS JOIN (
    SELECT 1 shirt_no, 'GOALKEEPER' position, 'STARTER' lineup_role UNION ALL
    SELECT 2, 'DEFENDER', 'STARTER' UNION ALL
    SELECT 3, 'DEFENDER', 'STARTER' UNION ALL
    SELECT 4, 'DEFENDER', 'STARTER' UNION ALL
    SELECT 5, 'DEFENDER', 'STARTER' UNION ALL
    SELECT 6, 'MIDFIELDER', 'STARTER' UNION ALL
    SELECT 7, 'MIDFIELDER', 'STARTER' UNION ALL
    SELECT 8, 'MIDFIELDER', 'STARTER' UNION ALL
    SELECT 9, 'FORWARD', 'STARTER' UNION ALL
    SELECT 10, 'FORWARD', 'STARTER' UNION ALL
    SELECT 11, 'FORWARD', 'STARTER' UNION ALL
    SELECT 12, 'GOALKEEPER', 'SUBSTITUTE' UNION ALL
    SELECT 13, 'DEFENDER', 'SUBSTITUTE' UNION ALL
    SELECT 14, 'DEFENDER', 'SUBSTITUTE' UNION ALL
    SELECT 15, 'MIDFIELDER', 'SUBSTITUTE' UNION ALL
    SELECT 16, 'MIDFIELDER', 'SUBSTITUTE' UNION ALL
    SELECT 17, 'FORWARD', 'SUBSTITUTE' UNION ALL
    SELECT 18, 'FORWARD', 'SUBSTITUTE'
) r
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    player_name = VALUES(player_name),
    position = VALUES(position),
    nationality = VALUES(nationality),
    birth_date = VALUES(birth_date),
    birth_year = VALUES(birth_year),
    player_status = 'ACTIVE',
    lineup_role = VALUES(lineup_role);

-- 每队1名主教练和2名助理教练；初始化时年龄为45至47岁。
INSERT INTO coach_info
    (club_id, coach_name, title, nationality, birth_year, description, coach_status)
SELECT c.club_id,
       CONCAT(c.short_name, CASE WHEN t.sort_no = 1 THEN '主教练' ELSE CONCAT('助理教练', t.sort_no - 1) END),
       t.title,
       c.nationality,
       YEAR(CURRENT_DATE) - (44 + t.sort_no),
       '课程设计正式初始化教练组成员。',
       'ACTIVE'
FROM (
    SELECT club_id, short_name,
           CASE club_name
               WHEN '曼城足球俱乐部' THEN '英国'
               WHEN '拜仁慕尼黑足球俱乐部' THEN '德国'
               WHEN '巴塞罗那足球俱乐部' THEN '西班牙'
               ELSE '美国'
           END AS nationality
    FROM club_info
    WHERE club_name IN (
        '曼城足球俱乐部',
        '拜仁慕尼黑足球俱乐部',
        '巴塞罗那足球俱乐部',
        '迈阿密国际足球俱乐部'
    )
) c
CROSS JOIN (
    SELECT 1 sort_no, 'HEAD_COACH' title UNION ALL
    SELECT 2, 'ASSISTANT_COACH' UNION ALL
    SELECT 3, 'ASSISTANT_COACH'
) t
WHERE NOT EXISTS (
    SELECT 1
    FROM coach_info existing
    WHERE existing.club_id = c.club_id
      AND existing.coach_name = CONCAT(c.short_name, CASE WHEN t.sort_no = 1 THEN '主教练' ELSE CONCAT('助理教练', t.sort_no - 1) END)
);
