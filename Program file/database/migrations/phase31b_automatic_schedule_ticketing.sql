-- Phase31-B: allow the automatic schedule publisher to create match ticket zones
-- without impersonating a human administrator. Existing creator ids are preserved.
ALTER TABLE match_ticket_zone
    MODIFY COLUMN created_by BIGINT UNSIGNED NULL COMMENT '创建管理员，NULL表示系统自动创建';
