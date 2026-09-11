USE league_ticket;

-- Schema-only migration. Preflight must prove there are no duplicate (phone, role_id) rows.
-- Existing ENABLED/DISABLED/LOCKED meanings are preserved; this migration never guesses a new status.
ALTER TABLE sys_user
    MODIFY COLUMN phone VARCHAR(20) NULL COMMENT '登录手机号；同一角色内唯一，允许跨角色复用',
    MODIFY COLUMN user_status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态',
    DROP INDEX uq_sys_user_phone,
    ADD CONSTRAINT uq_sys_user_phone_role UNIQUE (phone, role_id),
    DROP CHECK ck_sys_user_status,
    ADD CONSTRAINT ck_sys_user_status CHECK (
        user_status IN ('PENDING_ACTIVATION', 'PENDING_CLUB_APPROVAL', 'ENABLED', 'DISABLED', 'LOCKED')
    );
