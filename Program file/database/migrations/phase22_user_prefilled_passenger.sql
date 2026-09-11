USE league_ticket;

-- Phase22 only extends schema. It does not infer passenger data for historical orders.
CREATE TABLE ticket_passenger_identity (
    passenger_identity_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购票人稳定身份主键',
    id_card_no VARCHAR(32) NOT NULL COMMENT '身份证号，同一个购票人的稳定识别依据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (passenger_identity_id),
    CONSTRAINT uq_ticket_passenger_identity_card UNIQUE (id_card_no),
    CONSTRAINT ck_ticket_passenger_identity_card CHECK (CHAR_LENGTH(TRIM(id_card_no)) > 0)
) ENGINE=InnoDB COMMENT='预填购票人全局身份；全局占用数由用户关联数决定';

CREATE TABLE user_prefilled_passenger (
    prefilled_passenger_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户预填购票人主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属普通用户',
    passenger_identity_id BIGINT UNSIGNED NOT NULL COMMENT '稳定购票人身份',
    passenger_name VARCHAR(80) NOT NULL COMMENT '该用户保存的购票人姓名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (prefilled_passenger_id),
    CONSTRAINT uq_user_prefilled_passenger_identity UNIQUE (user_id, passenger_identity_id),
    CONSTRAINT fk_prefilled_passenger_user FOREIGN KEY (user_id) REFERENCES sys_user (user_id),
    CONSTRAINT fk_prefilled_passenger_identity FOREIGN KEY (passenger_identity_id) REFERENCES ticket_passenger_identity (passenger_identity_id),
    CONSTRAINT ck_prefilled_passenger_name CHECK (CHAR_LENGTH(TRIM(passenger_name)) > 0),
    KEY idx_prefilled_passenger_identity (passenger_identity_id)
) ENGINE=InnoDB COMMENT='普通用户预填购票人；每用户及每身份证号的4人限制由事务服务校验';

ALTER TABLE order_item
    ADD COLUMN passenger_name_snapshot VARCHAR(80) NULL COMMENT '购票时购票人姓名快照；历史订单保持NULL' AFTER seat_no_snapshot,
    ADD COLUMN passenger_id_card_snapshot VARCHAR(32) NULL COMMENT '购票时身份证号快照；历史订单保持NULL' AFTER passenger_name_snapshot;
