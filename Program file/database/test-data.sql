-- 足球联赛购票系统：正式初始化数据只读验收
-- 使用方式：先执行 schema.sql 和 seed.sql，再执行本脚本。
-- 本脚本不写入数据；任一规则不满足时返回 FAIL，便于人工或 CI 验收。

USE league_ticket;

SELECT 'ADMIN数量=1' AS check_item,
       IF(COUNT(*) = 1, 'PASS', CONCAT('FAIL: ', COUNT(*))) AS result
FROM sys_user u
JOIN sys_role r ON r.role_id = u.role_id
WHERE r.role_code = 'ADMIN';

SELECT 'USER数量=0' AS check_item,
       IF(COUNT(*) = 0, 'PASS', CONCAT('FAIL: ', COUNT(*))) AS result
FROM sys_user u
JOIN sys_role r ON r.role_id = u.role_id
WHERE r.role_code = 'USER';

SELECT 'EVENT_ADMIN数量=0' AS check_item,
       IF(COUNT(*) = 0, 'PASS', CONCAT('FAIL: ', COUNT(*))) AS result
FROM sys_user u
JOIN sys_role r ON r.role_id = u.role_id
WHERE r.role_code = 'EVENT_ADMIN';

SELECT 'CLUB数量=4' AS check_item,
       IF(COUNT(*) = 4, 'PASS', CONCAT('FAIL: ', COUNT(*))) AS result
FROM sys_user u
JOIN sys_role r ON r.role_id = u.role_id
WHERE r.role_code = 'CLUB';

SELECT '账号总数=5' AS check_item,
       IF(COUNT(*) = 5, 'PASS', CONCAT('FAIL: ', COUNT(*))) AS result
FROM sys_user;

SELECT 'CLUB账号全部绑定且使用BCrypt哈希' AS check_item,
       IF(
           COUNT(*) = 4
           AND COUNT(DISTINCT u.club_id) = 4
           AND SUM(u.club_id IS NULL OR u.user_status <> 'ENABLED') = 0
           AND SUM(u.password_hash NOT REGEXP '^\\$2[aby]\\$[0-9]{2}\\$') = 0,
           'PASS', 'FAIL'
       ) AS result
FROM sys_user u
JOIN sys_role r ON r.role_id = u.role_id
WHERE r.role_code = 'CLUB';

SELECT c.club_name,
       SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'STARTER') AS starters,
       SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'STARTER' AND p.position = 'GOALKEEPER') AS starting_goalkeepers,
       SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'STARTER' AND p.position = 'DEFENDER') AS starting_defenders,
       SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'STARTER' AND p.position = 'MIDFIELDER') AS starting_midfielders,
       SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'STARTER' AND p.position = 'FORWARD') AS starting_forwards,
       SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'SUBSTITUTE') AS substitutes,
       IF(
           SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'STARTER') = 11
           AND SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'STARTER' AND p.position = 'GOALKEEPER') = 1
           AND SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'STARTER' AND p.position = 'DEFENDER') = 4
           AND SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'STARTER' AND p.position = 'MIDFIELDER') = 3
           AND SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'STARTER' AND p.position = 'FORWARD') = 3
           AND SUM(p.player_status = 'ACTIVE' AND p.lineup_role = 'SUBSTITUTE') <= 7
           AND SUM(p.player_status = 'ACTIVE' AND (p.birth_year IS NULL OR YEAR(CURRENT_DATE) - p.birth_year NOT BETWEEN 18 AND 50)) = 0,
           'PASS', 'FAIL'
       ) AS player_result
FROM club_info c
LEFT JOIN player_info p ON p.club_id = c.club_id
WHERE c.club_name IN (
    '曼城足球俱乐部',
    '拜仁慕尼黑足球俱乐部',
    '巴塞罗那足球俱乐部',
    '迈阿密国际足球俱乐部'
)
GROUP BY c.club_id, c.club_name
ORDER BY c.club_id;

SELECT c.club_name,
       s.stadium_name,
       s.venue_model,
       s.capacity,
       cfg.vip_price,
       cfg.normal_price,
       COUNT(DISTINCT z.stadium_zone_id) AS active_zones,
       COUNT(seat.stadium_seat_id) AS active_seats,
       IF(
           c.home_stadium_id = cfg.stadium_id
           AND s.venue_model = 'STANDARD_8'
           AND s.stadium_status = 'ACTIVE'
           AND s.capacity = (cfg.long_side_seats_per_row + cfg.short_side_seats_per_row) * 2 * cfg.rows_per_zone * 2
           AND cfg.vip_price > cfg.normal_price
           AND COUNT(DISTINCT z.stadium_zone_id) = 8
           AND COUNT(DISTINCT CONCAT(z.zone_direction, '_', z.ticket_type)) = 8
           AND COUNT(seat.stadium_seat_id) = s.capacity
           AND SUM(seat.seat_status <> 'ACTIVE') = 0,
           'PASS', 'FAIL'
       ) AS stadium_result
FROM club_info c
JOIN club_home_stadium_config cfg ON cfg.club_id = c.club_id
JOIN stadium_info s ON s.stadium_id = cfg.stadium_id
LEFT JOIN stadium_zone z ON z.stadium_id = s.stadium_id AND z.zone_status = 'ACTIVE'
LEFT JOIN stadium_seat seat ON seat.stadium_zone_id = z.stadium_zone_id
WHERE c.club_name IN (
    '曼城足球俱乐部',
    '拜仁慕尼黑足球俱乐部',
    '巴塞罗那足球俱乐部',
    '迈阿密国际足球俱乐部'
)
GROUP BY c.club_id, c.club_name, c.home_stadium_id,
         s.stadium_id, s.stadium_name, s.venue_model, s.stadium_status, s.capacity,
         cfg.stadium_id, cfg.rows_per_zone, cfg.long_side_seats_per_row,
         cfg.short_side_seats_per_row, cfg.vip_price, cfg.normal_price
ORDER BY c.club_id;

SELECT c.club_name,
       SUM(co.coach_status = 'ACTIVE' AND co.title = 'HEAD_COACH') AS head_coaches,
       SUM(co.coach_status = 'ACTIVE' AND co.title = 'ASSISTANT_COACH') AS assistant_coaches,
       IF(
           SUM(co.coach_status = 'ACTIVE' AND co.title = 'HEAD_COACH') = 1
           AND SUM(co.coach_status = 'ACTIVE' AND co.title = 'ASSISTANT_COACH') <= 2
           AND SUM(co.coach_status = 'ACTIVE' AND (co.birth_year IS NULL OR YEAR(CURRENT_DATE) - co.birth_year NOT BETWEEN 18 AND 100)) = 0,
           'PASS', 'FAIL'
       ) AS coach_result
FROM club_info c
LEFT JOIN coach_info co ON co.club_id = c.club_id
WHERE c.club_name IN (
    '曼城足球俱乐部',
    '拜仁慕尼黑足球俱乐部',
    '巴塞罗那足球俱乐部',
    '迈阿密国际足球俱乐部'
)
GROUP BY c.club_id, c.club_name
ORDER BY c.club_id;

SELECT '正式俱乐部数量=4' AS check_item,
       IF(
           COUNT(*) = 4
           AND SUM(club_name IN (
               '曼城足球俱乐部',
               '拜仁慕尼黑足球俱乐部',
               '巴塞罗那足球俱乐部',
               '迈阿密国际足球俱乐部'
           )) = 4,
           'PASS', CONCAT('FAIL: ', COUNT(*))
       ) AS result
FROM club_info;

SELECT '根管理员使用BCrypt哈希' AS check_item,
       IF(COUNT(*) = 1, 'PASS', CONCAT('FAIL: ', COUNT(*))) AS result
FROM sys_user u
JOIN sys_role r ON r.role_id = u.role_id
WHERE r.role_code = 'ADMIN'
  AND u.username = 'root_admin'
  AND u.password_hash REGEXP '^\\$2[aby]\\$[0-9]{2}\\$';
