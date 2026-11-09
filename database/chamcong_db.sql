-- ============================================================
-- HttpSocketChamCong — MySQL schema + demo data
-- Chạy trong phpMyAdmin hoặc MySQL Workbench, hoặc:
--   mysql -u root -p < database/chamcong_db.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS chamcong_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chamcong_db;

-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    role       ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. CHẤM CÔNG
-- ============================================================
CREATE TABLE IF NOT EXISTS chamcong (
    username  VARCHAR(50) NOT NULL,
    work_date DATE        NOT NULL,
    check_in  TIME        NULL,
    check_out TIME        NULL,
    PRIMARY KEY (username, work_date),
    CONSTRAINT fk_chamcong_user
        FOREIGN KEY (username) REFERENCES users(username)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. ĐƠN XIN NGHỈ PHÉP
-- ============================================================
CREATE TABLE IF NOT EXISTS leave_requests (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    reason     TEXT        NULL,
    status     ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_user
        FOREIGN KEY (username) REFERENCES users(username)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_leave_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. CA LÀM VIỆC
-- ============================================================
CREATE TABLE IF NOT EXISTS work_shifts (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) NOT NULL,
    shift_date DATE        NOT NULL,
    shift_type ENUM('SANG', 'CHIEU', 'TOI') NOT NULL,
    note       VARCHAR(255) NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shift_user
        FOREIGN KEY (username) REFERENCES users(username)
        ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY uq_shift (username, shift_date, shift_type),
    INDEX idx_shift_date (shift_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. GHI CHÚ CÔNG VIỆC
-- ============================================================
CREATE TABLE IF NOT EXISTS work_notes (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) NOT NULL,
    note_date  DATE        NOT NULL,
    content    TEXT        NOT NULL,
    plan       TEXT        NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_user
        FOREIGN KEY (username) REFERENCES users(username)
        ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY uq_note_day (username, note_date),
    INDEX idx_note_date (note_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- DEMO USERS (mật khẩu demo — đồ án dùng plain text để dễ test)
-- ============================================================
INSERT IGNORE INTO users (username, password, full_name, role) VALUES
('chamnee',   'Chamnee@2025',  'Nguyễn Thị Bích Trâm', 'ADMIN'),
('nhanvien1', 'Nhanvien1@',    'Nguyễn Văn An',        'USER'),
('nhanvien2', 'Nhanvien2@',    'Trần Thị Bình',        'USER'),
('nhanvien3', 'Nhanvien3@',    'Lê Hoàng Cường',       'USER'),
('nhanvien4', 'Nhanvien4@',    'Phạm Thị Dung',        'USER'),
('nhanvien5', 'Nhanvien5@',    'Võ Minh Đức',           'USER');

-- ============================================================
-- DEMO CHẤM CÔNG (các ngày trước — hôm nay để trống cho test check-in)
-- ============================================================
INSERT IGNORE INTO chamcong (username, work_date, check_in, check_out) VALUES
('chamnee',   CURDATE() - INTERVAL 1 DAY, '07:55:00', '17:10:00'),
('nhanvien1', CURDATE() - INTERVAL 1 DAY, '07:58:00', '17:05:00'),
('nhanvien2', CURDATE() - INTERVAL 1 DAY, '08:12:00', '17:02:00'),
('nhanvien3', CURDATE() - INTERVAL 1 DAY, '07:45:00', '16:58:00'),
('nhanvien4', CURDATE() - INTERVAL 1 DAY, '08:05:00', '17:15:00'),
('nhanvien5', CURDATE() - INTERVAL 1 DAY, '07:50:00', '17:00:00'),

('chamnee',   CURDATE() - INTERVAL 2 DAY, '07:52:00', '17:08:00'),
('nhanvien1', CURDATE() - INTERVAL 2 DAY, '08:01:00', '17:04:00'),
('nhanvien2', CURDATE() - INTERVAL 2 DAY, '07:59:00', '17:20:00'),
('nhanvien3', CURDATE() - INTERVAL 2 DAY, '08:20:00', '17:00:00'),
('nhanvien4', CURDATE() - INTERVAL 2 DAY, '07:48:00', '16:55:00'),

('chamnee',   CURDATE() - INTERVAL 3 DAY, '07:57:00', '17:03:00'),
('nhanvien1', CURDATE() - INTERVAL 3 DAY, '07:56:00', '17:06:00'),
('nhanvien2', CURDATE() - INTERVAL 3 DAY, '08:03:00', '17:01:00'),
('nhanvien5', CURDATE() - INTERVAL 3 DAY, '07:44:00', '17:12:00'),

('chamnee',   CURDATE() - INTERVAL 4 DAY, '07:59:00', '17:00:00'),
('nhanvien1', CURDATE() - INTERVAL 4 DAY, '08:07:00', '17:09:00'),
('nhanvien3', CURDATE() - INTERVAL 4 DAY, '07:51:00', '17:05:00');

-- ============================================================
-- DEMO ĐƠN NGHỈ PHÉP
-- ============================================================
INSERT IGNORE INTO leave_requests (id, username, start_date, end_date, reason, status) VALUES
(1, 'nhanvien1', CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 4 DAY, 'Về quê có việc gia đình', 'PENDING'),
(2, 'nhanvien2', CURDATE() + INTERVAL 7 DAY, CURDATE() + INTERVAL 7 DAY, 'Khám sức khỏe định kỳ', 'PENDING'),
(3, 'nhanvien3', CURDATE() - INTERVAL 5 DAY, CURDATE() - INTERVAL 5 DAY, 'Nghỉ phép năm', 'APPROVED');

-- ============================================================
-- DEMO CA LÀM VIỆC
-- ============================================================
INSERT IGNORE INTO work_shifts (id, username, shift_date, shift_type, note) VALUES
(1, 'nhanvien1', CURDATE() + INTERVAL 1 DAY, 'SANG',  'Ca sáng tại văn phòng'),
(2, 'nhanvien1', CURDATE() + INTERVAL 1 DAY, 'CHIEU', 'Họp nhóm dự án'),
(3, 'nhanvien2', CURDATE() + INTERVAL 2 DAY, 'TOI',   'Hỗ trợ khách hàng'),
(4, 'nhanvien3', CURDATE() + INTERVAL 3 DAY, 'SANG',  'Trực quầy');

-- ============================================================
-- DEMO GHI CHÚ CÔNG VIỆC
-- ============================================================
INSERT IGNORE INTO work_notes (username, note_date, content, plan) VALUES
('nhanvien1', CURDATE() - INTERVAL 1 DAY, 'Hoàn thành báo cáo tuần, kiểm tra dữ liệu chấm công tháng.', 'Chuẩn bị nội dung họp nhóm vào sáng mai.'),
('nhanvien2', CURDATE() - INTERVAL 2 DAY, 'Hỗ trợ khách hàng qua điện thoại và email.', 'Theo dõi phản hồi của khách hàng trong ngày.'),
('nhanvien3', CURDATE() - INTERVAL 3 DAY, 'Cập nhật tài liệu hướng dẫn sử dụng hệ thống.', 'Rà soát lại quy trình chấm công.');
