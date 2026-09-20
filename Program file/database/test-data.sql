-- 足球联赛购票系统：新版演示数据
-- 使用方式：先执行 schema.sql 和 seed.sql，再执行本脚本。
-- 本脚本会清空业务演示数据并重新生成适配新版流程的数据。

USE league_ticket;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM checkin_record;
DELETE FROM refund_apply;
DELETE FROM e_ticket;
DELETE FROM payment_record;
DELETE FROM order_item;
DELETE FROM ticket_order;
DELETE FROM match_seat_inventory;
DELETE FROM match_ticket_zone;
DELETE FROM season_schedule_match;
DELETE FROM match_result_review;
DELETE FROM match_result_submission;
DELETE FROM season_schedule_batch;
DELETE FROM match_info;
DELETE FROM club_season_enrollment_coach;
DELETE FROM club_season_enrollment_player;
DELETE FROM club_season_enrollment;
DELETE FROM club_season_record;
DELETE FROM user_prefilled_passenger;
DELETE FROM ticket_passenger_identity;
DELETE FROM operation_log;
DELETE FROM sys_user;
DELETE FROM player_season_stat;
DELETE FROM coach_info;
DELETE FROM player_info;
DELETE FROM club_home_stadium_config;
DELETE FROM club_info;
DELETE FROM stadium_seat;
DELETE FROM stadium_zone;
DELETE FROM stadium_info;
DELETE FROM round_info;
DELETE FROM season_info;

ALTER TABLE checkin_record AUTO_INCREMENT = 1;
ALTER TABLE refund_apply AUTO_INCREMENT = 1;
ALTER TABLE e_ticket AUTO_INCREMENT = 1;
ALTER TABLE payment_record AUTO_INCREMENT = 1;
ALTER TABLE order_item AUTO_INCREMENT = 1;
ALTER TABLE ticket_order AUTO_INCREMENT = 1;
ALTER TABLE match_seat_inventory AUTO_INCREMENT = 1;
ALTER TABLE match_ticket_zone AUTO_INCREMENT = 1;
ALTER TABLE season_schedule_batch AUTO_INCREMENT = 1;
ALTER TABLE match_result_submission AUTO_INCREMENT = 1;
ALTER TABLE club_season_enrollment_coach AUTO_INCREMENT = 1;
ALTER TABLE club_season_enrollment_player AUTO_INCREMENT = 1;
ALTER TABLE club_season_enrollment AUTO_INCREMENT = 1;
ALTER TABLE club_season_record AUTO_INCREMENT = 1;
ALTER TABLE user_prefilled_passenger AUTO_INCREMENT = 1;
ALTER TABLE ticket_passenger_identity AUTO_INCREMENT = 1;
ALTER TABLE operation_log AUTO_INCREMENT = 1;
ALTER TABLE sys_user AUTO_INCREMENT = 1;
ALTER TABLE player_season_stat AUTO_INCREMENT = 1;
ALTER TABLE coach_info AUTO_INCREMENT = 1;
ALTER TABLE player_info AUTO_INCREMENT = 1;
ALTER TABLE club_info AUTO_INCREMENT = 1;
ALTER TABLE stadium_seat AUTO_INCREMENT = 1;
ALTER TABLE stadium_zone AUTO_INCREMENT = 1;
ALTER TABLE stadium_info AUTO_INCREMENT = 1;
ALTER TABLE round_info AUTO_INCREMENT = 1;
ALTER TABLE season_info AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

DROP TEMPORARY TABLE IF EXISTS _demo_num;
DROP TEMPORARY TABLE IF EXISTS _demo_num2;
CREATE TEMPORARY TABLE _demo_num (n INT NOT NULL PRIMARY KEY);
INSERT INTO _demo_num (n)
SELECT tens.n * 10 + ones.n
FROM (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
WHERE tens.n * 10 + ones.n BETWEEN 1 AND 99;
CREATE TEMPORARY TABLE _demo_num2 (n INT NOT NULL PRIMARY KEY);
INSERT INTO _demo_num2 (n)
SELECT n FROM _demo_num;

SET @role_user := (SELECT role_id FROM sys_role WHERE role_code = 'USER');
SET @role_club := (SELECT role_id FROM sys_role WHERE role_code = 'CLUB');
SET @role_event_admin := (SELECT role_id FROM sys_role WHERE role_code = 'EVENT_ADMIN');
SET @role_admin := (SELECT role_id FROM sys_role WHERE role_code = 'ADMIN');

INSERT INTO stadium_info
    (stadium_name, city, address, capacity, layout_description, venue_model, stadium_status)
VALUES
    ('杭州潮汐标准主场', '杭州', '滨江区潮汐路1号', 192, '标准8票区：东/西/南/北四个方向均分VIP和普通票区。', 'STANDARD_8', 'ACTIVE'),
    ('苏州园林标准主场', '苏州', '工业园区园林路2号', 192, '标准8票区：东/西/南/北四个方向均分VIP和普通票区。', 'STANDARD_8', 'ACTIVE'),
    ('杭州星火标准主场', '杭州', '钱塘区星火路3号', 192, '标准8票区：东/西/南/北四个方向均分VIP和普通票区。', 'STANDARD_8', 'ACTIVE'),
    ('苏州远航标准主场', '苏州', '高新区远航路4号', 192, '标准8票区：东/西/南/北四个方向均分VIP和普通票区。', 'STANDARD_8', 'ACTIVE');

SET @stadium_tide := (SELECT stadium_id FROM stadium_info WHERE stadium_name = '杭州潮汐标准主场');
SET @stadium_garden := (SELECT stadium_id FROM stadium_info WHERE stadium_name = '苏州园林标准主场');
SET @stadium_spark := (SELECT stadium_id FROM stadium_info WHERE stadium_name = '杭州星火标准主场');
SET @stadium_voyage := (SELECT stadium_id FROM stadium_info WHERE stadium_name = '苏州远航标准主场');

INSERT INTO club_info
    (club_name, short_name, home_city, home_address, home_stadium_id, description, club_status)
VALUES
    ('杭州潮汐足球俱乐部', '杭州潮汐', '杭州', '滨江区潮汐路1号', @stadium_tide, '新版演示俱乐部A。', 'ACTIVE'),
    ('苏州园林足球俱乐部', '苏州园林', '苏州', '工业园区园林路2号', @stadium_garden, '新版演示俱乐部B。', 'ACTIVE'),
    ('杭州星火足球俱乐部', '杭州星火', '杭州', '钱塘区星火路3号', @stadium_spark, '新版演示俱乐部C。', 'ACTIVE'),
    ('苏州远航足球俱乐部', '苏州远航', '苏州', '高新区远航路4号', @stadium_voyage, '新版演示俱乐部D。', 'ACTIVE');

SET @club_tide := (SELECT club_id FROM club_info WHERE club_name = '杭州潮汐足球俱乐部');
SET @club_garden := (SELECT club_id FROM club_info WHERE club_name = '苏州园林足球俱乐部');
SET @club_spark := (SELECT club_id FROM club_info WHERE club_name = '杭州星火足球俱乐部');
SET @club_voyage := (SELECT club_id FROM club_info WHERE club_name = '苏州远航足球俱乐部');

INSERT INTO club_home_stadium_config
    (club_id, stadium_id, rows_per_zone, long_side_seats_per_row, short_side_seats_per_row, vip_price, normal_price)
VALUES
    (@club_tide, @stadium_tide, 3, 8, 8, 200.00, 100.00),
    (@club_garden, @stadium_garden, 3, 8, 8, 200.00, 100.00),
    (@club_spark, @stadium_spark, 3, 8, 8, 200.00, 100.00),
    (@club_voyage, @stadium_voyage, 3, 8, 8, 200.00, 100.00);

INSERT INTO stadium_zone
    (stadium_id, zone_code, zone_name, zone_direction, ticket_type, sort_order, zone_status, description)
SELECT s.stadium_id,
       CONCAT(d.direction_code, '_', t.ticket_type),
       CONCAT(d.direction_name, ' ', CASE WHEN t.ticket_type = 'VIP' THEN 'VIP' ELSE '普通' END),
       d.direction_code,
       t.ticket_type,
       d.direction_order * 10 + t.type_order,
       'ACTIVE',
       CONCAT(d.direction_name, CASE WHEN t.ticket_type = 'VIP' THEN '近场VIP票区' ELSE '普通票区' END)
FROM stadium_info s
CROSS JOIN (
    SELECT 'EAST' direction_code, '东' direction_name, 1 direction_order UNION ALL
    SELECT 'WEST', '西', 2 UNION ALL
    SELECT 'SOUTH', '南', 3 UNION ALL
    SELECT 'NORTH', '北', 4
) d
CROSS JOIN (
    SELECT 'VIP' ticket_type, 1 type_order UNION ALL
    SELECT 'NORMAL', 2
) t
WHERE s.venue_model = 'STANDARD_8';

INSERT INTO stadium_seat
    (stadium_id, stadium_zone_id, row_no, row_seq, seat_no, seat_seq, center_distance, seat_status)
SELECT z.stadium_id,
       z.stadium_zone_id,
       CONCAT(CASE WHEN z.ticket_type = 'VIP' THEN r.n ELSE r.n + cfg.rows_per_zone END, '排'),
       CASE WHEN z.ticket_type = 'VIP' THEN r.n ELSE r.n + cfg.rows_per_zone END,
       CONCAT(seat_no.n, '座'),
       seat_no.n,
       CASE WHEN z.ticket_type = 'VIP' THEN r.n ELSE r.n + cfg.rows_per_zone END * 10
           + ABS(seat_no.n - ((CASE WHEN z.zone_direction IN ('EAST', 'WEST')
                                    THEN cfg.long_side_seats_per_row
                                    ELSE cfg.short_side_seats_per_row END + 1) / 2)),
       'ACTIVE'
FROM stadium_zone z
JOIN club_home_stadium_config cfg ON cfg.stadium_id = z.stadium_id
JOIN _demo_num r ON r.n BETWEEN 1 AND cfg.rows_per_zone
JOIN _demo_num2 seat_no ON seat_no.n BETWEEN 1 AND
    CASE WHEN z.zone_direction IN ('EAST', 'WEST')
         THEN cfg.long_side_seats_per_row
         ELSE cfg.short_side_seats_per_row END
WHERE z.zone_status = 'ACTIVE';

INSERT INTO sys_user
    (username, phone, password_hash, display_name, club_apply_name, employee_no, role_id, club_id, user_status)
VALUES
    ('demo_user', '13800000001', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '演示普通用户', NULL, NULL, @role_user, NULL, 'ENABLED'),
    ('demo_admin', '13800000002', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '演示系统管理员', NULL, 'SA0001', @role_admin, NULL, 'ENABLED'),
    ('demo_club', '13800000003', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '潮汐俱乐部负责人', '杭州潮汐足球俱乐部', NULL, @role_club, @club_tide, 'ENABLED'),
    ('demo_event_admin', '13800000005', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '演示赛事管理员', NULL, 'EA0001', @role_event_admin, NULL, 'ENABLED'),
    ('demo_club_garden', '13800000006', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '园林俱乐部负责人', '苏州园林足球俱乐部', NULL, @role_club, @club_garden, 'ENABLED'),
    ('demo_club_spark', '13800000007', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '星火俱乐部负责人', '杭州星火足球俱乐部', NULL, @role_club, @club_spark, 'ENABLED'),
    ('demo_club_voyage', '13800000008', 'DEMO_PASSWORD_NOT_FOR_LOGIN', '远航俱乐部负责人', '苏州远航足球俱乐部', NULL, @role_club, @club_voyage, 'ENABLED');

SET @demo_user_id := (SELECT user_id FROM sys_user WHERE phone = '13800000001' AND role_id = @role_user);
SET @demo_admin_id := (SELECT user_id FROM sys_user WHERE phone = '13800000002' AND role_id = @role_admin);
SET @demo_event_admin_id := (SELECT user_id FROM sys_user WHERE phone = '13800000005' AND role_id = @role_event_admin);

INSERT INTO coach_info
    (club_id, coach_name, title, nationality, birth_year, description, coach_status)
SELECT c.club_id,
       CONCAT(c.short_name, '主教练'),
       'HEAD_COACH',
       '中国',
       1978 + c.sort_no,
       '新版演示主教练',
       'ACTIVE'
FROM (
    SELECT @club_tide club_id, '潮汐' short_name, 1 sort_no UNION ALL
    SELECT @club_garden, '园林', 2 UNION ALL
    SELECT @club_spark, '星火', 3 UNION ALL
    SELECT @club_voyage, '远航', 4
) c;

INSERT INTO player_info
    (club_id, player_name, shirt_no, position, nationality, birth_date, birth_year, player_status, lineup_role)
SELECT c.club_id,
       CONCAT(c.short_name, '球员', LPAD(n.n, 2, '0')),
       n.n,
       CASE
           WHEN n.n IN (1, 12) THEN 'GOALKEEPER'
           WHEN n.n BETWEEN 2 AND 5 OR n.n = 13 THEN 'DEFENDER'
           WHEN n.n BETWEEN 6 AND 8 OR n.n = 14 THEN 'MIDFIELDER'
           ELSE 'FORWARD'
       END,
       '中国',
       DATE_ADD('1996-01-01', INTERVAL n.n DAY),
       1996 + MOD(n.n, 7),
       'ACTIVE',
       CASE WHEN n.n <= 11 THEN 'STARTER' ELSE 'SUBSTITUTE' END
FROM (
    SELECT @club_tide club_id, '潮汐' short_name UNION ALL
    SELECT @club_garden, '园林' UNION ALL
    SELECT @club_spark, '星火' UNION ALL
    SELECT @club_voyage, '远航'
) c
JOIN _demo_num n ON n.n BETWEEN 1 AND 16;

INSERT INTO season_info
    (season_name, start_date, end_date, registration_start_time, registration_deadline, ticket_sale_start_time, max_clubs, season_status, description)
VALUES
    ('2026新版演示赛季', '2026-09-20', '2026-10-31', '2026-09-01 00:00:00', '2026-09-18 19:59:00', NULL, 4, 'ACTIVE', '用于购票、退票和赛程展示的新版演示赛季。'),
    ('2026报名测试赛季', '2026-11-15', '2026-12-31', '2026-11-01 00:00:00', '2026-11-10 19:59:00', NULL, 4, 'DRAFT', '用于俱乐部赛季报名测试。');

SET @season_main_id := (SELECT season_id FROM season_info WHERE season_name = '2026新版演示赛季');
SET @season_signup_id := (SELECT season_id FROM season_info WHERE season_name = '2026报名测试赛季');

INSERT INTO round_info
    (season_id, round_no, round_name, start_date, end_date, round_status)
VALUES
    (@season_main_id, 1, '第1轮', '2026-09-20', '2026-09-21', 'PUBLISHED'),
    (@season_main_id, 2, '第2轮', '2026-09-27', '2026-09-28', 'PUBLISHED'),
    (@season_main_id, 3, '第3轮', '2026-10-04', '2026-10-05', 'PUBLISHED'),
    (@season_main_id, 4, '第4轮', '2026-10-11', '2026-10-12', 'PUBLISHED'),
    (@season_main_id, 5, '第5轮', '2026-10-18', '2026-10-19', 'PUBLISHED'),
    (@season_main_id, 6, '第6轮', '2026-10-25', '2026-10-26', 'PUBLISHED');

INSERT INTO club_season_record
    (season_id, club_id, played, wins, draws, losses, goals_for, goals_against, points, ranking)
VALUES
    (@season_main_id, @club_tide, 0, 0, 0, 0, 0, 0, 0, 1),
    (@season_main_id, @club_garden, 0, 0, 0, 0, 0, 0, 0, 2),
    (@season_main_id, @club_spark, 0, 0, 0, 0, 0, 0, 0, 3),
    (@season_main_id, @club_voyage, 0, 0, 0, 0, 0, 0, 0, 4);

INSERT INTO match_info
    (season_id, round_id, home_club_id, away_club_id, stadium_id, match_time, sale_start_time, sale_end_time, match_status, published_at)
SELECT @season_main_id,
       r.round_id,
       home.club_id,
       away.club_id,
       home.home_stadium_id,
       CAST(m.match_time AS DATETIME),
       DATE_ADD(DATE_SUB(DATE(CAST(m.match_time AS DATETIME)), INTERVAL 14 DAY), INTERVAL 20 HOUR),
       DATE_SUB(CAST(m.match_time AS DATETIME), INTERVAL 1 HOUR),
       'PUBLISHED',
       '2026-09-06 20:00:00'
FROM (
    SELECT 1 round_no, '杭州潮汐足球俱乐部' home_name, '苏州园林足球俱乐部' away_name, '2026-09-20 19:30:00' match_time UNION ALL
    SELECT 1, '杭州星火足球俱乐部', '苏州远航足球俱乐部', '2026-09-21 19:30:00' UNION ALL
    SELECT 2, '杭州潮汐足球俱乐部', '杭州星火足球俱乐部', '2026-09-27 19:30:00' UNION ALL
    SELECT 2, '苏州园林足球俱乐部', '苏州远航足球俱乐部', '2026-09-28 19:30:00' UNION ALL
    SELECT 3, '杭州潮汐足球俱乐部', '苏州远航足球俱乐部', '2026-10-04 19:30:00' UNION ALL
    SELECT 3, '苏州园林足球俱乐部', '杭州星火足球俱乐部', '2026-10-05 19:30:00' UNION ALL
    SELECT 4, '苏州园林足球俱乐部', '杭州潮汐足球俱乐部', '2026-10-11 19:30:00' UNION ALL
    SELECT 4, '苏州远航足球俱乐部', '杭州星火足球俱乐部', '2026-10-12 19:30:00' UNION ALL
    SELECT 5, '杭州星火足球俱乐部', '杭州潮汐足球俱乐部', '2026-10-18 19:30:00' UNION ALL
    SELECT 5, '苏州远航足球俱乐部', '苏州园林足球俱乐部', '2026-10-19 19:30:00' UNION ALL
    SELECT 6, '苏州远航足球俱乐部', '杭州潮汐足球俱乐部', '2026-10-25 19:30:00' UNION ALL
    SELECT 6, '杭州星火足球俱乐部', '苏州园林足球俱乐部', '2026-10-26 19:30:00'
) m
JOIN round_info r ON r.season_id = @season_main_id AND r.round_no = m.round_no
JOIN club_info home ON home.club_name = m.home_name
JOIN club_info away ON away.club_name = m.away_name;

INSERT INTO season_schedule_batch
    (season_id, batch_status, trigger_type, club_count, round_count, match_count, generated_at, confirmed_at, confirmed_by)
VALUES
    (@season_main_id, 'CONFIRMED', 'MANUAL', 4, 6, 12, '2026-09-06 20:00:00', '2026-09-06 20:05:00', @demo_event_admin_id);

SET @schedule_batch_id := (SELECT batch_id FROM season_schedule_batch WHERE season_id = @season_main_id);

INSERT INTO season_schedule_match (batch_id, match_id)
SELECT @schedule_batch_id, match_id
FROM match_info
WHERE season_id = @season_main_id;

INSERT INTO match_ticket_zone
    (match_id, stadium_zone_id, created_by, zone_name_snapshot, ticket_price, zone_status, sale_start_time, sale_end_time)
SELECT m.match_id,
       z.stadium_zone_id,
       @demo_event_admin_id,
       z.zone_name,
       CASE WHEN z.ticket_type = 'VIP' THEN cfg.vip_price ELSE cfg.normal_price END,
       'ON_SALE',
       m.sale_start_time,
       m.sale_end_time
FROM match_info m
JOIN stadium_zone z ON z.stadium_id = m.stadium_id AND z.zone_status = 'ACTIVE'
JOIN club_home_stadium_config cfg ON cfg.stadium_id = m.stadium_id
WHERE m.season_id = @season_main_id;

INSERT INTO match_seat_inventory
    (match_id, match_zone_id, stadium_seat_id, inventory_status)
SELECT mz.match_id,
       mz.match_zone_id,
       ss.stadium_seat_id,
       'AVAILABLE'
FROM match_ticket_zone mz
JOIN stadium_seat ss
  ON ss.stadium_zone_id = mz.stadium_zone_id
 AND ss.seat_status = 'ACTIVE';

INSERT INTO club_season_enrollment
    (season_id, club_id, stadium_id, enrollment_status, submitted_at)
SELECT @season_main_id, c.club_id, c.home_stadium_id, 'SUBMITTED', '2026-09-02 09:00:00'
FROM club_info c;

INSERT INTO club_season_enrollment_player
    (enrollment_id, player_id, lineup_role, player_name_snapshot, shirt_no_snapshot, position_snapshot, birth_date_snapshot, birth_year_snapshot, nationality_snapshot)
SELECT e.enrollment_id,
       p.player_id,
       p.lineup_role,
       p.player_name,
       p.shirt_no,
       p.position,
       p.birth_date,
       p.birth_year,
       p.nationality
FROM club_season_enrollment e
JOIN player_info p ON p.club_id = e.club_id
WHERE e.season_id = @season_main_id
  AND p.player_status = 'ACTIVE';

INSERT INTO club_season_enrollment_coach
    (enrollment_id, coach_id, coach_name_snapshot, title_snapshot, birth_year_snapshot, nationality_snapshot)
SELECT e.enrollment_id,
       c.coach_id,
       c.coach_name,
       c.title,
       c.birth_year,
       c.nationality
FROM club_season_enrollment e
JOIN coach_info c ON c.club_id = e.club_id
WHERE e.season_id = @season_main_id
  AND c.coach_status = 'ACTIVE';

INSERT INTO ticket_passenger_identity (id_card_no)
VALUES
    ('330100200001010011'),
    ('330100200002020022'),
    ('330100200003030033');

INSERT INTO user_prefilled_passenger
    (user_id, passenger_identity_id, passenger_name)
SELECT @demo_user_id, pi.passenger_identity_id, p.passenger_name
FROM (
    SELECT '330100200001010011' id_card_no, '演示购票人一' passenger_name UNION ALL
    SELECT '330100200002020022', '演示购票人二' UNION ALL
    SELECT '330100200003030033', '演示购票人三'
) p
JOIN ticket_passenger_identity pi ON pi.id_card_no = p.id_card_no;

DROP TEMPORARY TABLE IF EXISTS _demo_num;
DROP TEMPORARY TABLE IF EXISTS _demo_num2;
