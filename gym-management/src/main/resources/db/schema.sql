-- =====================================================================
-- 健身房管理系统 数据库结构（H2 / MySQL 通用，不含建库语句）
-- 说明：字段命名统一小写下划线，主键统一 id BIGINT 自增，全表带 create_time
-- =====================================================================

DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS equipment;
DROP TABLE IF EXISTS body_record;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS course_schedule;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS card_recharge;
DROP TABLE IF EXISTS member_card;
DROP TABLE IF EXISTS card_type;
DROP TABLE IF EXISTS coach;
DROP TABLE IF EXISTS member;
DROP TABLE IF EXISTS sys_user;

-- ---------------------------------------------------------------------
-- 1. 系统账号表
-- ---------------------------------------------------------------------
CREATE TABLE sys_user (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    real_name       VARCHAR(50)  NOT NULL,
    phone           VARCHAR(20),
    status          TINYINT      NOT NULL DEFAULT 1,
    last_login_time DATETIME,
    create_time     DATETIME     NOT NULL
);
CREATE UNIQUE INDEX uk_user_username ON sys_user (username);
CREATE INDEX idx_user_role ON sys_user (role);

-- ---------------------------------------------------------------------
-- 2. 会员信息表
-- ---------------------------------------------------------------------
CREATE TABLE member (
    id         BIGINT      PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    member_no  VARCHAR(20) NOT NULL,
    name       VARCHAR(50) NOT NULL,
    gender     TINYINT     NOT NULL DEFAULT 1,
    birthday   DATE,
    phone      VARCHAR(20) NOT NULL,
    id_card    VARCHAR(30),
    height     DECIMAL(5,1),
    join_date  DATE        NOT NULL,
    source     VARCHAR(20),
    remark     VARCHAR(255),
    status     TINYINT     NOT NULL DEFAULT 1,
    create_time DATETIME   NOT NULL,
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
);
CREATE UNIQUE INDEX uk_member_user ON member (user_id);
CREATE UNIQUE INDEX uk_member_no ON member (member_no);
CREATE INDEX idx_member_phone ON member (phone);
CREATE INDEX idx_member_join ON member (join_date);

-- ---------------------------------------------------------------------
-- 3. 教练信息表
-- ---------------------------------------------------------------------
CREATE TABLE coach (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    coach_no    VARCHAR(20)  NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    gender      TINYINT      NOT NULL DEFAULT 1,
    phone       VARCHAR(20)  NOT NULL,
    specialty   VARCHAR(50),
    coach_level VARCHAR(20),
    hire_date   DATE,
    hourly_wage DECIMAL(8,2),
    intro       VARCHAR(500),
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL,
    CONSTRAINT fk_coach_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
);
CREATE UNIQUE INDEX uk_coach_user ON coach (user_id);
CREATE UNIQUE INDEX uk_coach_no ON coach (coach_no);
CREATE INDEX idx_coach_specialty ON coach (specialty);

-- ---------------------------------------------------------------------
-- 4. 会员卡类型表
-- ---------------------------------------------------------------------
CREATE TABLE card_type (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    type_name   VARCHAR(50)  NOT NULL,
    card_kind   VARCHAR(20)  NOT NULL,
    valid_days  INT          NOT NULL DEFAULT 0,
    total_times INT          NOT NULL DEFAULT 0,
    price       DECIMAL(10,2) NOT NULL,
    description VARCHAR(255),
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL
);
CREATE UNIQUE INDEX uk_card_type_name ON card_type (type_name);

-- ---------------------------------------------------------------------
-- 5. 会员持卡表
-- ---------------------------------------------------------------------
CREATE TABLE member_card (
    id            BIGINT      PRIMARY KEY AUTO_INCREMENT,
    card_no       VARCHAR(30) NOT NULL,
    member_id     BIGINT      NOT NULL,
    card_type_id  BIGINT      NOT NULL,
    start_date    DATE        NOT NULL,
    end_date      DATE,
    total_times   INT         NOT NULL DEFAULT 0,
    remain_times  INT         NOT NULL DEFAULT 0,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    freeze_reason VARCHAR(100),
    renew_count   INT         NOT NULL DEFAULT 0,
    create_time   DATETIME    NOT NULL,
    CONSTRAINT fk_card_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_card_type FOREIGN KEY (card_type_id) REFERENCES card_type (id)
);
CREATE UNIQUE INDEX uk_card_no ON member_card (card_no);
CREATE INDEX idx_card_member ON member_card (member_id);
CREATE INDEX idx_card_end_date ON member_card (end_date);
CREATE INDEX idx_card_status ON member_card (status);

-- ---------------------------------------------------------------------
-- 6. 会员卡缴费流水表
-- ---------------------------------------------------------------------
CREATE TABLE card_recharge (
    id             BIGINT        PRIMARY KEY AUTO_INCREMENT,
    recharge_no    VARCHAR(30)   NOT NULL,
    member_card_id BIGINT        NOT NULL,
    member_id      BIGINT        NOT NULL,
    card_type_id   BIGINT        NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    pay_type       VARCHAR(20)   NOT NULL,
    add_days       INT           NOT NULL DEFAULT 0,
    add_times      INT           NOT NULL DEFAULT 0,
    operator_id    BIGINT,
    remark         VARCHAR(255),
    create_time    DATETIME      NOT NULL,
    CONSTRAINT fk_recharge_card FOREIGN KEY (member_card_id) REFERENCES member_card (id),
    CONSTRAINT fk_recharge_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE UNIQUE INDEX uk_recharge_no ON card_recharge (recharge_no);
CREATE INDEX idx_recharge_time ON card_recharge (create_time);
CREATE INDEX idx_recharge_member ON card_recharge (member_id);

-- ---------------------------------------------------------------------
-- 7. 课程信息表
-- ---------------------------------------------------------------------
CREATE TABLE course (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    course_name VARCHAR(50)  NOT NULL,
    course_type VARCHAR(20)  NOT NULL,
    difficulty  TINYINT      NOT NULL DEFAULT 1,
    duration    INT          NOT NULL,
    capacity    INT          NOT NULL,
    price       DECIMAL(8,2) NOT NULL DEFAULT 0,
    coach_id    BIGINT,
    description VARCHAR(255),
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL
);
CREATE INDEX idx_course_name ON course (course_name);
CREATE INDEX idx_course_type ON course (course_type);
CREATE INDEX idx_course_status ON course (status);

-- ---------------------------------------------------------------------
-- 8. 排课表
-- ---------------------------------------------------------------------
CREATE TABLE course_schedule (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT,
    course_id    BIGINT      NOT NULL,
    coach_id     BIGINT      NOT NULL,
    class_date   DATE        NOT NULL,
    start_time   TIME        NOT NULL,
    end_time     TIME        NOT NULL,
    room         VARCHAR(30) NOT NULL,
    capacity     INT         NOT NULL,
    booked_count INT         NOT NULL DEFAULT 0,
    position_total INT      NOT NULL DEFAULT 0,
    status       VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    remark       VARCHAR(255),
    create_time  DATETIME    NOT NULL,
    CONSTRAINT fk_schedule_course FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT fk_schedule_coach FOREIGN KEY (coach_id) REFERENCES coach (id)
);
CREATE UNIQUE INDEX uk_schedule_slot ON course_schedule (coach_id, class_date, start_time);
CREATE INDEX idx_schedule_date ON course_schedule (class_date);
CREATE INDEX idx_schedule_status ON course_schedule (status);

-- ---------------------------------------------------------------------
-- 9. 预约签到表
-- ---------------------------------------------------------------------
CREATE TABLE reservation (
    id             BIGINT      PRIMARY KEY AUTO_INCREMENT,
    reserve_no     VARCHAR(30) NOT NULL,
    schedule_id    BIGINT      NOT NULL,
    member_id      BIGINT      NOT NULL,
    member_card_id BIGINT,
    status         VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    position_no    INT,
    cancel_type    VARCHAR(20),
    reserve_time   DATETIME    NOT NULL,
    check_in_time  DATETIME,
    cancel_time    DATETIME,
    remark         VARCHAR(255),
    create_time    DATETIME    NOT NULL,
    CONSTRAINT fk_reserve_schedule FOREIGN KEY (schedule_id) REFERENCES course_schedule (id),
    CONSTRAINT fk_reserve_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE UNIQUE INDEX uk_reserve_schedule_member ON reservation (schedule_id, member_id);
CREATE UNIQUE INDEX uk_reserve_no ON reservation (reserve_no);
CREATE INDEX idx_reserve_member ON reservation (member_id);
CREATE INDEX idx_reserve_status ON reservation (status);

-- ---------------------------------------------------------------------
-- 10. 体测记录表
-- ---------------------------------------------------------------------
CREATE TABLE body_record (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT,
    member_id   BIGINT      NOT NULL,
    record_date DATE        NOT NULL,
    height      DECIMAL(5,1) NOT NULL,
    weight      DECIMAL(5,1) NOT NULL,
    bmi         DECIMAL(4,1) NOT NULL,
    body_fat    DECIMAL(4,1),
    muscle      DECIMAL(5,1),
    waist       DECIMAL(5,1),
    heart_rate  INT,
    coach_id    BIGINT,
    remark      VARCHAR(255),
    create_time DATETIME    NOT NULL,
    CONSTRAINT fk_body_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE INDEX idx_body_member_date ON body_record (member_id, record_date);

-- ---------------------------------------------------------------------
-- 11. 器材台账表
-- ---------------------------------------------------------------------
CREATE TABLE equipment (
    id                 BIGINT       PRIMARY KEY AUTO_INCREMENT,
    equipment_no       VARCHAR(30)  NOT NULL,
    equipment_name     VARCHAR(50)  NOT NULL,
    category           VARCHAR(20)  NOT NULL,
    brand              VARCHAR(50),
    purchase_date      DATE,
    price              DECIMAL(10,2),
    location           VARCHAR(50),
    status             VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    fault_desc         VARCHAR(255),
    report_time        DATETIME,
    repair_finish_time DATETIME,
    repair_cost        DECIMAL(8,2),
    remark             VARCHAR(255),
    create_time        DATETIME     NOT NULL
);
CREATE UNIQUE INDEX uk_equipment_no ON equipment (equipment_no);
CREATE INDEX idx_equipment_category ON equipment (category);
CREATE INDEX idx_equipment_status ON equipment (status);

-- ---------------------------------------------------------------------
-- 12. 公告表
-- ---------------------------------------------------------------------
CREATE TABLE notice (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(100) NOT NULL,
    content      VARCHAR(2000) NOT NULL,
    publisher_id BIGINT,
    target_role  VARCHAR(20)  NOT NULL DEFAULT 'ALL',
    status       TINYINT      NOT NULL DEFAULT 1,
    publish_time DATETIME,
    view_count   INT          NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL
);
CREATE INDEX idx_notice_status ON notice (status, publish_time);

-- ---------------------------------------------------------------------
-- 13. 到店记录表（区分自助训练与团课签到，保证到店率口径完整）
-- ---------------------------------------------------------------------
CREATE TABLE checkin_log (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT,
    member_id    BIGINT      NOT NULL,
    checkin_type VARCHAR(20) NOT NULL,
    ref_id       BIGINT,
    create_time  DATETIME    NOT NULL,
    CONSTRAINT fk_checkin_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE INDEX idx_checkin_member ON checkin_log (member_id, create_time);

-- ---------------------------------------------------------------------
-- 14. 候补队列表
-- ---------------------------------------------------------------------
CREATE TABLE waitlist (
    id             BIGINT      PRIMARY KEY AUTO_INCREMENT,
    schedule_id    BIGINT      NOT NULL,
    member_id      BIGINT      NOT NULL,
    member_card_id BIGINT,
    status         VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    enqueue_time   DATETIME    NOT NULL,
    create_time    DATETIME    NOT NULL,
    CONSTRAINT fk_wait_schedule FOREIGN KEY (schedule_id) REFERENCES course_schedule (id),
    CONSTRAINT fk_wait_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE UNIQUE INDEX uk_wait_schedule_member ON waitlist (schedule_id, member_id);
CREATE INDEX idx_wait_schedule ON waitlist (schedule_id, status, enqueue_time);

-- ---------------------------------------------------------------------
-- 15. 会员预警表（到期 / 流失，支持回填处理结果）
-- ---------------------------------------------------------------------
CREATE TABLE member_alert (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    member_id    BIGINT       NOT NULL,
    alert_type   VARCHAR(20)  NOT NULL,
    content      VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    handle_note  VARCHAR(255),
    create_time  DATETIME     NOT NULL,
    handle_time  DATETIME,
    CONSTRAINT fk_alert_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE INDEX idx_alert_member ON member_alert (member_id, status);
