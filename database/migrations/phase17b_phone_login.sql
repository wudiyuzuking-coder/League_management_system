-- 阶段17B：手机号成为唯一登录凭证，username改为允许重复的昵称。
-- 执行前必须检查查询结果；若存在ENABLED空手机号或重复手机号，应停止迁移并人工处理。
USE league_ticket;

SELECT u.user_id, u.username, u.phone, u.user_status, r.role_code
FROM sys_user u
JOIN sys_role r ON r.role_id = u.role_id
WHERE u.phone IS NULL OR TRIM(u.phone) = '';

SELECT phone, COUNT(*) AS duplicate_count
FROM sys_user
WHERE phone IS NOT NULL AND TRIM(phone) <> ''
GROUP BY phone
HAVING COUNT(*) > 1;

ALTER TABLE sys_user
    DROP INDEX uq_sys_user_username,
    ADD INDEX idx_sys_user_username (username);

-- phone唯一约束uq_sys_user_phone保持不变；本阶段为历史兼容不改为NOT NULL。
