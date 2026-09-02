-- 阶段17A：在阶段16D数据库上增加管理人员工号独立字段。
-- 历史管理账号保持NULL，由管理员在启用或编辑前人工补齐；不根据username猜测工号。
USE league_ticket;

ALTER TABLE sys_user
    ADD COLUMN employee_no VARCHAR(16) NULL
        COMMENT '管理人员工号；EVENT_ADMIN为EA加4位数字，ADMIN为SA加4位数字'
        AFTER display_name;

UPDATE sys_user
SET employee_no = CASE username
    WHEN 'demo_event_admin' THEN 'EA0001'
    WHEN 'demo_admin' THEN 'SA0001'
    ELSE employee_no
END
WHERE username IN ('demo_event_admin', 'demo_admin');

-- 结果应为空；若存在记录，应先人工处理重复值再执行后续唯一约束。
SELECT employee_no, COUNT(*) AS duplicate_count
FROM sys_user
WHERE employee_no IS NOT NULL
GROUP BY employee_no
HAVING COUNT(*) > 1;

ALTER TABLE sys_user
    ADD CONSTRAINT uq_sys_user_employee_no UNIQUE (employee_no);
