-- 足球联赛购票系统：系统基础数据
-- 可重复执行；仅维护固定角色、基础权限、角色授权与系统参数。

USE league_ticket;

INSERT INTO sys_role (role_code, role_name, role_status, remark)
VALUES
    ('USER', '普通用户', 'ENABLED', '浏览比赛、购票及管理本人订单'),
    ('CLUB', '俱乐部管理员', 'ENABLED', '维护本俱乐部资料'),
    ('CHECKER', '检票员', 'ENABLED', '执行比赛现场检票'),
    ('ADMIN', '系统管理员', 'ENABLED', '管理全联赛业务数据')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    role_status = VALUES(role_status),
    remark = VALUES(remark);

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
    ('REFUND_AUDIT', '审核退票', 'ENABLED', '审核用户整单退票申请'),
    ('STATISTICS_VIEW', '查看统计', 'ENABLED', '查看上座率、销售额等统计数据'),
    ('USER_MANAGE', '管理用户', 'ENABLED', '管理用户、角色和账号状态')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_status = VALUES(permission_status),
    description = VALUES(description);

-- 普通用户权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('MATCH_VIEW', 'ORDER_CREATE', 'ORDER_VIEW_SELF', 'REFUND_APPLY')
WHERE r.role_code = 'USER';

-- 俱乐部管理员权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('MATCH_VIEW', 'CLUB_MANAGE_SELF', 'STATISTICS_VIEW')
WHERE r.role_code = 'CLUB';

-- 检票员权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('MATCH_VIEW', 'CHECKIN')
WHERE r.role_code = 'CHECKER';

-- 系统管理员拥有当前全部基础权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'ADMIN';

INSERT INTO sys_config (config_key, config_value, value_type, description, config_status)
VALUES
    ('ORDER_PAYMENT_TIMEOUT_MINUTES', '15', 'INTEGER', '待支付订单和锁座的超时时长（分钟）', 'ENABLED'),
    ('SALE_STOP_BEFORE_MINUTES', '30', 'INTEGER', '比赛开始前停止售票的时间（分钟）', 'ENABLED'),
    ('REFUND_STOP_BEFORE_HOURS', '24', 'INTEGER', '比赛开始前停止申请退票的时间（小时）', 'ENABLED'),
    ('MAX_TICKETS_PER_ORDER', '4', 'INTEGER', '单笔订单最大购票张数', 'ENABLED')
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    value_type = VALUES(value_type),
    description = VALUES(description),
    config_status = VALUES(config_status);
