-- 足球联赛购票系统：课程设计最小演示数据
-- 密码字段故意使用不可登录标记；阶段3接入正式密码哈希后必须替换。

USE league_ticket;

INSERT INTO season_info (season_name, start_date, end_date, registration_start_time, registration_deadline, max_clubs, season_status, description)
VALUES ('2026演示赛季', '2026-08-01', '2027-05-31', '2026-06-01 00:00:00', '2026-07-25 00:00:00', 4, 'ACTIVE', '软件课程设计演示赛季')
ON DUPLICATE KEY UPDATE registration_start_time=VALUES(registration_start_time),registration_deadline=VALUES(registration_deadline),max_clubs=VALUES(max_clubs),season_status=VALUES(season_status),description=VALUES(description);
SET @season_id := (SELECT season_id FROM season_info WHERE season_name = '2026演示赛季');

INSERT INTO season_info (season_name, start_date, end_date, registration_start_time, registration_deadline, max_clubs, season_status, description)
VALUES ('2027报名演示赛季', '2027-10-01', '2028-05-31', '2026-08-01 00:00:00', '2027-09-24 00:00:00', 4, 'DRAFT', '阶段16B CLUB赛季报名演示')
ON DUPLICATE KEY UPDATE registration_start_time=VALUES(registration_start_time),registration_deadline=VALUES(registration_deadline),max_clubs=VALUES(max_clubs),season_status=VALUES(season_status),description=VALUES(description);

INSERT INTO round_info (season_id, round_no, round_name, start_date, end_date, round_status)
VALUES
    (@season_id, 1, '第1轮', '2026-09-05', '2026-09-06', 'PUBLISHED'),
    (@season_id, 2, '第2轮', '2026-09-12', '2026-09-13', 'PUBLISHED')
ON DUPLICATE KEY UPDATE
    round_name = VALUES(round_name),
    start_date = VALUES(start_date),
    end_date = VALUES(end_date),
    round_status = VALUES(round_status);
SET @round_1_id := (SELECT round_id FROM round_info WHERE season_id = @season_id AND round_no = 1);
SET @round_2_id := (SELECT round_id FROM round_info WHERE season_id = @season_id AND round_no = 2);

INSERT INTO stadium_info (stadium_name, city, address, capacity, layout_description, stadium_status)
VALUES
    ('滨江足球场', '杭州', '滨江区演示路1号', 48, '东看台和西看台各3排，每排8座', 'ACTIVE'),
    ('湖畔体育场', '苏州', '工业园区演示路2号', 48, '南看台和北看台各3排，每排8座', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    address = VALUES(address), capacity = VALUES(capacity), layout_description = VALUES(layout_description), stadium_status = VALUES(stadium_status);
SET @stadium_1_id := (SELECT stadium_id FROM stadium_info WHERE stadium_name = '滨江足球场' AND city = '杭州');
SET @stadium_2_id := (SELECT stadium_id FROM stadium_info WHERE stadium_name = '湖畔体育场' AND city = '苏州');

INSERT INTO stadium_zone (stadium_id, zone_code, zone_name, sort_order, zone_status, description)
VALUES
    (@stadium_1_id, 'EAST', '东看台', 1, 'ACTIVE', '滨江足球场东侧看台'),
    (@stadium_1_id, 'WEST', '西看台', 2, 'ACTIVE', '滨江足球场西侧看台'),
    (@stadium_2_id, 'SOUTH', '南看台', 1, 'ACTIVE', '湖畔体育场南侧看台'),
    (@stadium_2_id, 'NORTH', '北看台', 2, 'ACTIVE', '湖畔体育场北侧看台')
ON DUPLICATE KEY UPDATE
    zone_name = VALUES(zone_name), sort_order = VALUES(sort_order), zone_status = VALUES(zone_status), description = VALUES(description);
SET @zone_1_east := (SELECT stadium_zone_id FROM stadium_zone WHERE stadium_id = @stadium_1_id AND zone_code = 'EAST');
SET @zone_1_west := (SELECT stadium_zone_id FROM stadium_zone WHERE stadium_id = @stadium_1_id AND zone_code = 'WEST');
SET @zone_2_south := (SELECT stadium_zone_id FROM stadium_zone WHERE stadium_id = @stadium_2_id AND zone_code = 'SOUTH');
SET @zone_2_north := (SELECT stadium_zone_id FROM stadium_zone WHERE stadium_id = @stadium_2_id AND zone_code = 'NORTH');

-- 每个场馆2个票区，每个票区3排，每排8座。物理座位只记录ACTIVE/DISABLED。
INSERT INTO stadium_seat
    (stadium_id, stadium_zone_id, row_no, row_seq, seat_no, seat_seq, center_distance, seat_status)
SELECT z.stadium_id, z.stadium_zone_id, CONCAT(r.row_seq, '排'), r.row_seq,
       CONCAT(s.seat_seq, '座'), s.seat_seq, ABS(s.seat_seq - 4.5), 'ACTIVE'
FROM stadium_zone z
CROSS JOIN (
    SELECT 1 AS row_seq UNION ALL SELECT 2 UNION ALL SELECT 3
) r
CROSS JOIN (
    SELECT 1 AS seat_seq UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) s
WHERE z.stadium_id IN (@stadium_1_id, @stadium_2_id)
ON DUPLICATE KEY UPDATE
    stadium_id = VALUES(stadium_id), center_distance = VALUES(center_distance), seat_status = VALUES(seat_status);

INSERT INTO club_info
    (club_name, short_name, home_city, home_address, home_stadium_id, description, club_status)
VALUES
    ('杭州潮汐足球俱乐部', '杭州潮汐', '杭州', '滨江足球场', @stadium_1_id, '演示俱乐部A', 'ACTIVE'),
    ('苏州园林足球俱乐部', '苏州园林', '苏州', '湖畔体育场', @stadium_2_id, '演示俱乐部B', 'ACTIVE'),
    ('杭州星火足球俱乐部', '杭州星火', '杭州', '滨江足球场', @stadium_1_id, '演示俱乐部C', 'ACTIVE'),
    ('苏州远航足球俱乐部', '苏州远航', '苏州', '湖畔体育场', @stadium_2_id, '演示俱乐部D', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    short_name = VALUES(short_name), home_city = VALUES(home_city), home_address = VALUES(home_address),
    home_stadium_id = VALUES(home_stadium_id), description = VALUES(description), club_status = VALUES(club_status);
SET @club_a := (SELECT club_id FROM club_info WHERE club_name = '杭州潮汐足球俱乐部');
SET @club_b := (SELECT club_id FROM club_info WHERE club_name = '苏州园林足球俱乐部');
SET @club_c := (SELECT club_id FROM club_info WHERE club_name = '杭州星火足球俱乐部');
SET @club_d := (SELECT club_id FROM club_info WHERE club_name = '苏州远航足球俱乐部');

INSERT INTO coach_info (club_id, coach_name, title, nationality, description, coach_status)
SELECT @club_a, '陈教练', 'HEAD_COACH', '中国', '课程设计演示教练', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM coach_info WHERE club_id = @club_a AND coach_name = '陈教练' AND title = 'HEAD_COACH');

INSERT INTO coach_info (club_id, coach_name, title, nationality, description, coach_status)
SELECT x.club_id, CONCAT(x.prefix,'演示主教练'), 'HEAD_COACH', '中国', '自动排赛演示教练', 'ACTIVE'
FROM (
    SELECT @club_b club_id, '苏州园林' prefix UNION ALL
    SELECT @club_c, '杭州星火' UNION ALL
    SELECT @club_d, '苏州远航'
) x
WHERE NOT EXISTS (SELECT 1 FROM coach_info c WHERE c.club_id=x.club_id AND c.title='HEAD_COACH');

INSERT INTO player_info (club_id, player_name, shirt_no, position, nationality, player_status)
VALUES
    (@club_a, '演示前锋甲', 9, 'FORWARD', '中国', 'ACTIVE'),
    (@club_b, '演示门将乙', 1, 'GOALKEEPER', '中国', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    player_name = VALUES(player_name), position = VALUES(position), nationality = VALUES(nationality), player_status = VALUES(player_status);

INSERT INTO player_info (club_id, player_name, shirt_no, position, nationality, birth_date, player_status)
VALUES
    (@club_a,'演示门将1',1,'GOALKEEPER','中国','1998-01-10','ACTIVE'),
    (@club_a,'演示后卫2',2,'DEFENDER','中国','1999-02-11','ACTIVE'),
    (@club_a,'演示后卫3',3,'DEFENDER','中国','2000-03-12','ACTIVE'),
    (@club_a,'演示后卫4',4,'DEFENDER','中国','2001-04-13','ACTIVE'),
    (@club_a,'演示后卫5',5,'DEFENDER','中国','1997-05-14','ACTIVE'),
    (@club_a,'演示中场6',6,'MIDFIELDER','中国','1998-06-15','ACTIVE'),
    (@club_a,'演示中场7',7,'MIDFIELDER','中国','1999-07-16','ACTIVE'),
    (@club_a,'演示中场8',8,'MIDFIELDER','中国','2000-08-17','ACTIVE'),
    (@club_a,'演示前锋10',10,'FORWARD','中国','2001-09-18','ACTIVE'),
    (@club_a,'演示前锋11',11,'FORWARD','中国','1998-10-19','ACTIVE')
ON DUPLICATE KEY UPDATE player_name=VALUES(player_name),position=VALUES(position),nationality=VALUES(nationality),birth_date=VALUES(birth_date),player_status=VALUES(player_status);

INSERT INTO player_info (club_id, player_name, shirt_no, position, nationality, birth_date, player_status)
SELECT c.club_id,CONCAT(c.prefix,'演示球员',n.no),n.no,
       CASE WHEN n.no=1 THEN 'GOALKEEPER' WHEN n.no<=5 THEN 'DEFENDER' WHEN n.no<=8 THEN 'MIDFIELDER' ELSE 'FORWARD' END,
       '中国','2000-01-01','ACTIVE'
FROM (
    SELECT @club_b club_id,'苏州园林' prefix UNION ALL
    SELECT @club_c,'杭州星火' UNION ALL
    SELECT @club_d,'苏州远航'
) c
CROSS JOIN (
    SELECT 1 no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
    UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11
) n
WHERE 1=1
ON DUPLICATE KEY UPDATE player_name=VALUES(player_name),position=VALUES(position),nationality=VALUES(nationality),birth_date=VALUES(birth_date),player_status=VALUES(player_status);

INSERT INTO club_season_record (season_id, club_id, played, wins, draws, losses, goals_for, goals_against, points, ranking)
VALUES
    (@season_id, @club_a, 0, 0, 0, 0, 0, 0, 0, 1),
    (@season_id, @club_b, 0, 0, 0, 0, 0, 0, 0, 2),
    (@season_id, @club_c, 0, 0, 0, 0, 0, 0, 0, 3),
    (@season_id, @club_d, 0, 0, 0, 0, 0, 0, 0, 4)
ON DUPLICATE KEY UPDATE ranking = VALUES(ranking);

-- 四场比赛；有意不建立同赛季主客队组合唯一约束。
INSERT INTO match_info
    (season_id, round_id, home_club_id, away_club_id, stadium_id, match_time, sale_start_time, sale_end_time, match_status, published_at)
SELECT @season_id, @round_1_id, @club_a, @club_b, @stadium_1_id,
       '2026-09-05 19:30:00', '2026-08-25 10:00:00', '2026-09-05 19:00:00', 'PUBLISHED', '2026-08-25 09:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM match_info WHERE season_id = @season_id AND round_id = @round_1_id
      AND home_club_id = @club_a AND away_club_id = @club_b AND match_time = '2026-09-05 19:30:00'
);
INSERT INTO match_info
    (season_id, round_id, home_club_id, away_club_id, stadium_id, match_time, sale_start_time, sale_end_time, match_status, published_at)
SELECT @season_id, @round_1_id, @club_c, @club_d, @stadium_1_id,
       '2026-09-06 19:30:00', '2026-08-25 10:00:00', '2026-09-06 19:00:00', 'PUBLISHED', '2026-08-25 09:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM match_info WHERE season_id = @season_id AND round_id = @round_1_id
      AND home_club_id = @club_c AND away_club_id = @club_d AND match_time = '2026-09-06 19:30:00'
);
INSERT INTO match_info
    (season_id, round_id, home_club_id, away_club_id, stadium_id, match_time, sale_start_time, sale_end_time, match_status, published_at)
SELECT @season_id, @round_2_id, @club_a, @club_c, @stadium_1_id,
       '2026-09-12 19:30:00', '2026-08-25 10:00:00', '2026-09-12 19:00:00', 'PUBLISHED', '2026-08-25 09:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM match_info WHERE season_id = @season_id AND round_id = @round_2_id
      AND home_club_id = @club_a AND away_club_id = @club_c AND match_time = '2026-09-12 19:30:00'
);
INSERT INTO match_info
    (season_id, round_id, home_club_id, away_club_id, stadium_id, match_time, sale_start_time, sale_end_time, match_status, published_at)
SELECT @season_id, @round_2_id, @club_b, @club_d, @stadium_2_id,
       '2026-09-13 19:30:00', '2026-08-25 10:00:00', '2026-09-13 19:00:00', 'PUBLISHED', '2026-08-25 09:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM match_info WHERE season_id = @season_id AND round_id = @round_2_id
      AND home_club_id = @club_b AND away_club_id = @club_d AND match_time = '2026-09-13 19:30:00'
);

SET @match_1 := (SELECT match_id FROM match_info WHERE season_id = @season_id AND round_id = @round_1_id AND home_club_id = @club_a AND away_club_id = @club_b AND match_time = '2026-09-05 19:30:00');
SET @match_2 := (SELECT match_id FROM match_info WHERE season_id = @season_id AND round_id = @round_1_id AND home_club_id = @club_c AND away_club_id = @club_d AND match_time = '2026-09-06 19:30:00');
SET @match_3 := (SELECT match_id FROM match_info WHERE season_id = @season_id AND round_id = @round_2_id AND home_club_id = @club_a AND away_club_id = @club_c AND match_time = '2026-09-12 19:30:00');

-- 比赛票区需要记录创建管理员，因此测试账号必须先于票区数据写入。
SET @role_user := (SELECT role_id FROM sys_role WHERE role_code = 'USER');
SET @role_club := (SELECT role_id FROM sys_role WHERE role_code = 'CLUB');
SET @role_checker := (SELECT role_id FROM sys_role WHERE role_code = 'CHECKER');
SET @role_event_admin := (SELECT role_id FROM sys_role WHERE role_code = 'EVENT_ADMIN');
SET @role_admin := (SELECT role_id FROM sys_role WHERE role_code = 'ADMIN');

INSERT INTO sys_user (username, phone, password_hash, display_name, club_apply_name, employee_no, role_id, club_id, user_status)
VALUES
    ('demo_user', '13800000001', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '演示普通用户', NULL, NULL, @role_user, NULL, 'ENABLED'),
    ('demo_admin', '13800000002', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '演示系统管理员', NULL, 'SA0001', @role_admin, NULL, 'ENABLED'),
    ('demo_club', '13800000003', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '潮汐俱乐部负责人', '杭州潮汐足球俱乐部', NULL, @role_club, @club_a, 'ENABLED'),
    ('demo_event_admin', '13800000005', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '演示赛事管理员', NULL, 'EA0001', @role_event_admin, NULL, 'ENABLED')
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash), display_name = VALUES(display_name), club_apply_name = VALUES(club_apply_name),
    employee_no = VALUES(employee_no), role_id = VALUES(role_id),
    club_id = VALUES(club_id), user_status = VALUES(user_status);
SET @demo_admin_id := (SELECT user_id FROM sys_user WHERE phone = '13800000002');

UPDATE sys_user
SET display_name = '历史检票员账号',
    employee_no = NULL,
    role_id = @role_checker,
    club_id = @club_a,
    user_status = 'DISABLED'
WHERE username = 'demo_checker';

-- 第1场和第3场共用同一物理场馆，但各自拥有独立票区配置和座位库存。
INSERT INTO match_ticket_zone
    (match_id, stadium_zone_id, created_by, zone_name_snapshot, ticket_price, zone_status, sale_start_time, sale_end_time)
VALUES
    (@match_1, @zone_1_east, @demo_admin_id, '东看台', 120.00, 'ON_SALE', '2026-08-29 20:00:00', '2026-09-05 19:00:00'),
    (@match_1, @zone_1_west, @demo_admin_id, '西看台', 180.00, 'ON_SALE', '2026-08-29 20:00:00', '2026-09-05 19:00:00'),
    (@match_3, @zone_1_east, @demo_admin_id, '东看台', 100.00, 'ON_SALE', '2026-09-05 20:00:00', '2026-09-12 19:00:00'),
    (@match_3, @zone_1_west, @demo_admin_id, '西看台', 160.00, 'ON_SALE', '2026-09-05 20:00:00', '2026-09-12 19:00:00')
ON DUPLICATE KEY UPDATE
    zone_name_snapshot = VALUES(zone_name_snapshot), ticket_price = VALUES(ticket_price),
    zone_status = VALUES(zone_status),
    sale_start_time = VALUES(sale_start_time), sale_end_time = VALUES(sale_end_time);

-- 阶段20B2：演示票区开售时间始终由比赛时间计算，避免复制的常量漂移。
UPDATE match_ticket_zone mz
JOIN match_info m ON m.match_id = mz.match_id
SET mz.sale_start_time = DATE_ADD(DATE_SUB(DATE(m.match_time), INTERVAL 7 DAY), INTERVAL 20 HOUR)
WHERE mz.match_id IN (@match_1, @match_3);

INSERT INTO match_seat_inventory (match_id, match_zone_id, stadium_seat_id, inventory_status)
SELECT mz.match_id, mz.match_zone_id, ss.stadium_seat_id, 'AVAILABLE'
FROM match_ticket_zone mz
JOIN stadium_seat ss ON ss.stadium_zone_id = mz.stadium_zone_id AND ss.seat_status = 'ACTIVE'
WHERE mz.match_id IN (@match_1, @match_3)
ON DUPLICATE KEY UPDATE match_zone_id = VALUES(match_zone_id);

-- 阶段10不预置ticket_order/order_item演示记录，避免静默制造过期锁或与订单状态不一致的明细。
-- 订单数据由USER通过正式下单接口创建，order_item.item_status随业务事务写入。
