-- 阶段20B1：CLUB注册申请名称、审核自动建队和俱乐部唯一负责人。
-- 执行前不会自动清理任何历史账号；发现非CLUB绑定或重复绑定时立即终止。
USE league_ticket;

DELIMITER //
CREATE PROCEDURE phase20b1_assert_club_binding_safe()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys_user u
        JOIN sys_role r ON r.role_id = u.role_id
        WHERE u.club_id IS NOT NULL AND r.role_code <> 'CLUB'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'phase20b1 blocked: non-CLUB account uses club_id';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM sys_user
        WHERE club_id IS NOT NULL
        GROUP BY club_id
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'phase20b1 blocked: duplicate club leaders require manual review';
    END IF;
END//
DELIMITER ;

CALL phase20b1_assert_club_binding_safe();
DROP PROCEDURE phase20b1_assert_club_binding_safe;

ALTER TABLE club_info
    MODIFY COLUMN home_city VARCHAR(50) NULL
        COMMENT '俱乐部所在城市，NULL表示资料尚未完善';

ALTER TABLE sys_user
    ADD COLUMN club_apply_name VARCHAR(100) NULL
        COMMENT 'CLUB注册申请的俱乐部名称，仅用于审核阶段'
        AFTER display_name,
    ADD CONSTRAINT uq_sys_user_club UNIQUE (club_id);

-- 明确演示账号可安全回填；其他历史账号不得根据昵称或display_name自动推断。
UPDATE sys_user u
JOIN sys_role r ON r.role_id = u.role_id
LEFT JOIN club_info c ON c.club_id = u.club_id
SET u.club_apply_name = c.club_name
WHERE u.username = 'demo_club'
  AND u.phone = '13800000003'
  AND r.role_code = 'CLUB'
  AND u.club_id IS NOT NULL;
