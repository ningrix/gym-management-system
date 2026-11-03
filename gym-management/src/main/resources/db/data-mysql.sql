-- 使用前请先执行 schema.sql 建表（MySQL 8.0+，utf8mb4）
-- =====================================================================
-- 健身房管理系统 · 演示数据（MySQL 版，用于正式环境初始化）
-- 仅保留 1 名管理员、2 名会员、2 名教练、3 门课程与少量业务记录；
-- 日期基于运行当天自动推算，保证任何一天启动都能看到「今日课程」。
-- 口令：管理员 admin2026，会员/教练 gym2026
-- =====================================================================

-- 系统账号
INSERT INTO sys_user (id, username, password, role, real_name, phone, status, create_time) VALUES
(1,  'admin',    '9437757de048398b8fe94450943d562d4df424db5ae3d5b7e7aa1a32441e82a6', 'ADMIN',  '张伟强', '13906010001', 1, '2026-01-05 09:00:00'),
(2,  'member01', '97c4f77d2e0205d30f059dbe6fa29af72e5dc0178b8719736a94b0ee193822fc', 'MEMBER', '李明哲', '13806010002', 1, '2026-03-12 10:20:00'),
(3,  'member02', '97c4f77d2e0205d30f059dbe6fa29af72e5dc0178b8719736a94b0ee193822fc', 'MEMBER', '王雨欣', '13806010003', 1, '2026-08-01 15:40:00'),
(10, 'coach01',  '97c4f77d2e0205d30f059dbe6fa29af72e5dc0178b8719736a94b0ee193822fc', 'COACH',  '阮腾', '13906010010', 1, '2023-03-01 09:00:00'),
(11, 'coach02',  '97c4f77d2e0205d30f059dbe6fa29af72e5dc0178b8719736a94b0ee193822fc', 'COACH',  '林晓薇', '13906010011', 1, '2024-06-15 09:00:00');

-- 会员档案
INSERT INTO member (id, user_id, member_no, name, gender, birthday, phone, id_card, height, join_date, source, remark, status, create_time) VALUES
(1, 2, 'M2026001', '李明哲', 1, '1996-04-18', '13806010002', '350302199604180011', 175.0, DATE_ADD(CURRENT_DATE, INTERVAL -30 DAY), '到店咨询', '目标减脂，每周三次', 1, '2026-03-12 10:20:00'),
(2, 3, 'M2026002', '王雨欣', 0, '1999-09-02', '13806010003', '350302199909020022', 162.0, DATE_ADD(CURRENT_DATE, INTERVAL -10 DAY), '线上推广', '偏好瑜伽与普拉提', 1, '2026-08-01 15:40:00');

-- 教练档案
INSERT INTO coach (id, user_id, coach_no, name, gender, phone, specialty, coach_level, hire_date, hourly_wage, intro, status, create_time) VALUES
(1, 10, 'C001', '阮腾', 1, '13906010010', '增肌塑形', '高级', '2023-03-01', 180.00, '国家健身教练职业资格三级，擅长力量训练与体态矫正。', 1, '2023-03-01 09:00:00'),
(2, 11, 'C002', '林晓薇', 0, '13906010011', '瑜伽普拉提', '中级', '2024-06-15', 150.00, 'RYT200 认证瑜伽教练，主授流瑜伽与普拉提。', 1, '2024-06-15 09:00:00');

-- 卡类型（字典数据）
INSERT INTO card_type (id, type_name, card_kind, valid_days, total_times, price, description, status, create_time) VALUES
(1, '月卡',         'DURATION', 30,  0,  299.00,  '30 天内不限次参加团课，含体测一次', 1, '2026-01-05 09:10:00'),
(2, '季卡',         'DURATION', 90,  0,  799.00,  '90 天内不限次参加团课，含体测两次', 1, '2026-01-05 09:10:00'),
(3, '年卡',         'DURATION', 365, 0, 2599.00,  '365 天内不限次参加团课与自主训练', 1, '2026-01-05 09:10:00'),
(4, '次卡（20 次）', 'TIMES',    0,  20, 1200.00, '计次卡，20 次团课，不限使用期限', 1, '2026-01-05 09:10:00');

-- 会员持卡
INSERT INTO member_card (id, card_no, member_id, card_type_id, start_date, end_date, total_times, remain_times, status, freeze_reason, renew_count, create_time) VALUES
(1, 'MC2026001', 1, 3, DATE_ADD(CURRENT_DATE, INTERVAL -30 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 335 DAY), 0,  0,  'ACTIVE', NULL, 0, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -30 DAY)),
(2, 'MC2026002', 2, 4, DATE_ADD(CURRENT_DATE, INTERVAL -10 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 355 DAY), 20, 18, 'ACTIVE', NULL, 0, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -10 DAY));

-- 缴费流水
INSERT INTO card_recharge (id, recharge_no, member_card_id, member_id, card_type_id, amount, pay_type, add_days, add_times, operator_id, create_time) VALUES
(1, 'CR2026001', 1, 1, 3, 2599.00, 'WECHAT', 365, 0,  1, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -30 DAY)),
(2, 'CR2026002', 2, 2, 4, 1200.00, 'ALIPAY', 0,   20, 1, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -10 DAY));

-- 课程
INSERT INTO course (id, course_name, course_type, difficulty, duration, capacity, price, coach_id, description, status, create_time) VALUES
(1, '力量训练', 'STRENGTH', 2, 60, 12, 60.00, 1, '以深蹲、硬拉、卧推为主的自由重量训练，适合有基础的会员。', 1, '2026-01-06 09:00:00'),
(2, '动感单车', 'SPIN',     2, 45, 24, 40.00, 1, '节奏骑行与间歇冲刺结合，提升心肺耐力。', 1, '2026-01-06 09:10:00'),
(3, '流瑜伽',   'YOGA',     1, 60, 20, 50.00, 2, '以体式串联与呼吸控制为主，缓解久坐带来的紧张。', 1, '2026-01-06 09:20:00');

-- 排课（昨天 / 今天 / 明天 / 后天，保证任何一天启动都有今日课程）
INSERT INTO course_schedule (id, course_id, coach_id, class_date, start_time, end_time, room, capacity, booked_count, status, remark, create_time) VALUES
(1, 1, 1, DATE_ADD(CURRENT_DATE, INTERVAL -1 DAY), '19:00:00', '20:00:00', '力量区', 12, 1, 'FINISHED', NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -3 DAY)),
(2, 2, 1, CURRENT_DATE,                     '19:30:00', '20:15:00', '单车房', 24, 2, 'WAITING',  NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY)),
(3, 3, 2, CURRENT_DATE,                     '18:00:00', '19:00:00', '操房B',  20, 1, 'WAITING',  NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY)),
(4, 3, 2, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY),  '09:30:00', '10:30:00', '操房B',  20, 0, 'WAITING',  NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY)),
(5, 1, 1, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY),  '18:30:00', '19:30:00', '力量区', 12, 0, 'WAITING',  NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY));

-- 预约（一条已签到、三条待上课）
INSERT INTO reservation (id, reserve_no, schedule_id, member_id, member_card_id, status, reserve_time, check_in_time, cancel_time, remark, create_time) VALUES
(1, 'RS2026001', 1, 1, 1, 'CHECKED_IN', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), NULL, NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY)),
(2, 'RS2026002', 2, 1, 1, 'BOOKED',     DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), NULL, NULL, NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY)),
(3, 'RS2026003', 2, 2, 2, 'BOOKED',     DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), NULL, NULL, NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY)),
(4, 'RS2026004', 3, 2, 2, 'BOOKED',     DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -6 HOUR), NULL, NULL, NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -6 HOUR));

-- 体测记录
INSERT INTO body_record (id, member_id, record_date, height, weight, bmi, body_fat, muscle, waist, heart_rate, coach_id, remark, create_time) VALUES
(1, 1, DATE_ADD(CURRENT_DATE, INTERVAL -1 DAY), 175.0, 73.6, 24.0, 18.5, 34.2, 84.0, 68, 1, '减脂期第 4 周，体重较上月下降 1.8kg', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY)),
(2, 2, DATE_ADD(CURRENT_DATE, INTERVAL -2 DAY), 162.0, 55.2, 21.0, 24.1, 25.8, 68.0, 70, 2, '柔韧性提升明显，建议增加核心训练', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY));

-- 器材台账
INSERT INTO equipment (id, equipment_no, equipment_name, category, brand, purchase_date, price, location, status, fault_desc, report_time, repair_finish_time, repair_cost, remark, create_time) VALUES
(1, 'EQ0001', '商用跑步机', 'CARDIO',   '舒华',     '2024-05-10', 12800.00, '有氧区 A', 'NORMAL', NULL, NULL, NULL, 0.00, '日常巡检正常', '2024-05-10 10:00:00'),
(2, 'EQ0006', '史密斯机',   'STRENGTH', '英派斯',   '2024-05-10', 9600.00,  '力量区 B', 'NORMAL', NULL, NULL, NULL, 0.00, NULL, '2024-05-10 10:10:00'),
(3, 'EQ0009', '划船机',     'CARDIO',   'Concept2', '2025-03-18', 6800.00,  '有氧区 B', 'NORMAL', NULL, NULL, NULL, 0.00, NULL, '2025-03-18 14:00:00');

-- 公告
INSERT INTO notice (id, title, content, publisher_id, target_role, status, publish_time, view_count, create_time) VALUES
(1, '场馆营业时间调整通知', '自本周起，工作日营业时间调整为 06:00—22:30，周末 08:00—21:00，请合理安排训练时间。', 1, 'ALL',    1, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY), 12, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY)),
(2, '团课预约规则说明',     '团课可提前 7 天预约，开课前 2 小时停止预约；如需取消请在开课前操作，连续爽约将影响后续预约。', 1, 'MEMBER', 1, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), 8,  DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY));

-- 到店记录（团课签到 + 自助训练）
INSERT INTO checkin_log (id, member_id, checkin_type, ref_id, create_time) VALUES
(1, 1, 'COURSE', 1,    DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY)),
(2, 1, 'SELF',   NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -20 HOUR)),
(3, 2, 'SELF',   NULL, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -3 HOUR));
