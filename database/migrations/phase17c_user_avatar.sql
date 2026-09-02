-- 阶段17C：为已有数据库增加用户头像访问路径。
-- 图片文件保存在APP_UPLOAD_DIR指向的本地目录，数据库只保存相对访问路径。
USE league_ticket;

ALTER TABLE sys_user
    ADD COLUMN avatar_url VARCHAR(255) NULL COMMENT '用户头像访问路径' AFTER employee_no;
