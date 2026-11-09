-- ============================================================
-- HttpSocketChamCong — MySQL/MariaDB schema + dữ liệu mẫu thật
-- Chạy trong phpMyAdmin hoặc MySQL Workbench.
--
-- Lưu ý: file này DROP database cũ để tránh xung đột dữ liệu.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS chamcong_db;

CREATE DATABASE chamcong_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chamcong_db;

-- ============================================================
-- 1. USERS — nhân sự + phân quyền
-- ============================================================
CREATE TABLE users (
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    role       ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    department VARCHAR(100) NULL,
    phone      VARCHAR(20)  NULL,
    avatar_url VARCHAR(255) NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. CHẤM CÔNG — có GPS + FaceID mô phỏng
-- ============================================================
CREATE TABLE chamcong (
    username             VARCHAR(50) NOT NULL,
    work_date            DATE        NOT NULL,
    check_in             TIME        NULL,
    check_out            TIME        NULL,
    checkin_lat          DOUBLE      NULL,
    checkin_lng          DOUBLE      NULL,
    checkin_face_verified TINYINT(1) NOT NULL DEFAULT 0,
    note                 VARCHAR(255) NULL,
    PRIMARY KEY (username, work_date),
    CONSTRAINT fk_chamcong_user
        FOREIGN KEY (username) REFERENCES users(username)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_chamcong_date (work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. ĐƠN TỪ — nghỉ phép / đi muộn / công tác
-- ============================================================
CREATE TABLE leave_requests (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    reason     TEXT        NULL,
    leave_type ENUM('NGHI_PHEP', 'DI_MUON', 'CONG_TAC') NOT NULL DEFAULT 'NGHI_PHEP',
    status     ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_user
        FOREIGN KEY (username) REFERENCES users(username)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_leave_status (status),
    INDEX idx_leave_type (leave_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. CA LÀM VIỆC
-- ============================================================
CREATE TABLE work_shifts (
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
CREATE TABLE work_notes (
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
-- DỮ LIỆU MẪU — TÊN THẬT
-- Mật khẩu demo dùng plain text để dễ test đồ án.
-- ============================================================
INSERT INTO users (username, password, full_name, role, department, phone, avatar_url) VALUES
('admin',   'Admin@2025',   'Nguyễn Duy Minh',       'ADMIN', 'Phòng Kỹ thuật',          '0901 234 567', 'https://ui-avatars.com/api/?name=Nguyen+Duy+Minh&background=2563eb&color=fff'),
('pvkhai',  'Nhanvien@2025', 'Phan Văn Khải',        'USER',  'Phòng Kỹ thuật',          '0912 345 678', 'https://ui-avatars.com/api/?name=Phan+Van+Khai&background=0ea5e9&color=fff'),
('ttbtram', 'Nhanvien@2025', 'Trần Thị Bích Trâm',   'USER',  'Phòng Kinh doanh',        '0923 456 789', 'https://ui-avatars.com/api/?name=Tran+Thi+Bich+Tram&background=0284c7&color=fff'),
('lhcuong', 'Nhanvien@2025', 'Lê Hoàng Cường',       'USER',  'Phòng Kỹ thuật',          '0934 567 890', 'https://ui-avatars.com/api/?name=Le+Hoang+Cuong&background=1d4ed8&color=fff'),
('ptdung',  'Nhanvien@2025', 'Phạm Thị Dung',        'USER',  'Phòng Nhân sự',           '0945 678 901', 'https://ui-avatars.com/api/?name=Pham+Thi+Dung&background=0369a1&color=fff'),
('vmduc',   'Nhanvien@2025', 'Võ Minh Đức',          'USER',  'Phòng Chăm sóc khách hàng','0956 789 012', 'https://ui-avatars.com/api/?name=Vo+Minh+Duc&background=075985&color=fff');

-- ============================================================
-- CHẤM CÔNG 7 NGÀY GẦN NHẤT (tính cả hôm nay)
-- Có người đúng giờ, có người đi muộn; hôm nay để trống
-- pvkhai + ptdung để test CHECK-IN trên web.
-- ============================================================
INSERT INTO chamcong
(username, work_date, check_in, check_out, checkin_lat, checkin_lng, checkin_face_verified, note) VALUES
-- 6 ngày trước
('admin',   CURDATE() - INTERVAL 6 DAY, '07:58:00', '17:05:00', 21.02780, 105.83420, 1, ''),
('pvkhai',  CURDATE() - INTERVAL 6 DAY, '08:02:00', '17:10:00', 21.02810, 105.83450, 1, ''),
('ttbtram', CURDATE() - INTERVAL 6 DAY, '08:15:00', '17:00:00', 21.02740, 105.83390, 1, 'Đi muộn do tắc đường'),
('lhcuong', CURDATE() - INTERVAL 6 DAY, '07:50:00', '17:02:00', 21.02830, 105.83480, 1, ''),
('ptdung',  CURDATE() - INTERVAL 6 DAY, '07:55:00', '17:08:00', 21.02720, 105.83370, 1, ''),
('vmduc',   CURDATE() - INTERVAL 6 DAY, '08:05:00', '17:00:00', 21.02800, 105.83440, 1, ''),
-- 5 ngày trước
('admin',   CURDATE() - INTERVAL 5 DAY, '07:55:00', '17:00:00', 21.02780, 105.83420, 1, ''),
('pvkhai',  CURDATE() - INTERVAL 5 DAY, '07:59:00', '17:05:00', 21.02810, 105.83450, 1, ''),
('ttbtram', CURDATE() - INTERVAL 5 DAY, '08:20:00', '17:15:00', 21.02740, 105.83390, 1, 'Đi muộn 20 phút'),
('lhcuong', CURDATE() - INTERVAL 5 DAY, '07:45:00', '16:58:00', 21.02830, 105.83480, 1, ''),
('ptdung',  CURDATE() - INTERVAL 5 DAY, '07:57:00', '17:03:00', 21.02720, 105.83370, 1, ''),
('vmduc',   CURDATE() - INTERVAL 5 DAY, '08:01:00', '17:00:00', 21.02800, 105.83440, 1, ''),
-- 4 ngày trước
('admin',   CURDATE() - INTERVAL 4 DAY, '07:52:00', '17:06:00', 21.02780, 105.83420, 1, ''),
('pvkhai',  CURDATE() - INTERVAL 4 DAY, '08:03:00', '17:00:00', 21.02810, 105.83450, 1, ''),
('ttbtram', CURDATE() - INTERVAL 4 DAY, '07:58:00', '17:00:00', 21.02740, 105.83390, 1, ''),
('lhcuong', CURDATE() - INTERVAL 4 DAY, '08:12:00', '17:10:00', 21.02830, 105.83480, 1, 'Đi muộn do xe hỏng'),
('ptdung',  CURDATE() - INTERVAL 4 DAY, '07:49:00', '17:02:00', 21.02720, 105.83370, 1, ''),
('vmduc',   CURDATE() - INTERVAL 4 DAY, '07:55:00', '17:05:00', 21.02800, 105.83440, 1, ''),
-- 3 ngày trước
('admin',   CURDATE() - INTERVAL 3 DAY, '07:57:00', '17:00:00', 21.02780, 105.83420, 1, ''),
('pvkhai',  CURDATE() - INTERVAL 3 DAY, '07:56:00', '17:06:00', 21.02810, 105.83450, 1, ''),
('ttbtram', CURDATE() - INTERVAL 3 DAY, '08:05:00', '17:00:00', 21.02740, 105.83390, 1, ''),
('lhcuong', CURDATE() - INTERVAL 3 DAY, '07:55:00', '17:00:00', 21.02830, 105.83480, 1, ''),
('ptdung',  CURDATE() - INTERVAL 3 DAY, '08:18:00', '17:12:00', 21.02720, 105.83370, 1, 'Đi muộn 18 phút'),
('vmduc',   CURDATE() - INTERVAL 3 DAY, '07:50:00', '17:00:00', 21.02800, 105.83440, 1, ''),
-- 2 ngày trước
('admin',   CURDATE() - INTERVAL 2 DAY, '07:59:00', '17:00:00', 21.02780, 105.83420, 1, ''),
('pvkhai',  CURDATE() - INTERVAL 2 DAY, '08:07:00', '17:09:00', 21.02810, 105.83450, 1, 'Đi muộn 7 phút'),
('ttbtram', CURDATE() - INTERVAL 2 DAY, '07:57:00', '17:00:00', 21.02740, 105.83390, 1, ''),
('lhcuong', CURDATE() - INTERVAL 2 DAY, '07:48:00', '17:03:00', 21.02830, 105.83480, 1, ''),
('ptdung',  CURDATE() - INTERVAL 2 DAY, '07:52:00', '17:00:00', 21.02720, 105.83370, 1, ''),
('vmduc',   CURDATE() - INTERVAL 2 DAY, '08:04:00', '17:00:00', 21.02800, 105.83440, 1, ''),
-- hôm qua
('admin',   CURDATE() - INTERVAL 1 DAY, '07:55:00', '17:05:00', 21.02780, 105.83420, 1, ''),
('pvkhai',  CURDATE() - INTERVAL 1 DAY, '07:58:00', '17:02:00', 21.02810, 105.83450, 1, ''),
('ttbtram', CURDATE() - INTERVAL 1 DAY, '08:10:00', '17:00:00', 21.02740, 105.83390, 1, 'Đi muộn 10 phút'),
('lhcuong', CURDATE() - INTERVAL 1 DAY, '07:45:00', '16:55:00', 21.02830, 105.83480, 1, ''),
('ptdung',  CURDATE() - INTERVAL 1 DAY, '07:50:00', '17:07:00', 21.02720, 105.83370, 1, ''),
('vmduc',   CURDATE() - INTERVAL 1 DAY, '08:00:00', '17:00:00', 21.02800, 105.83440, 1, ''),
-- hôm nay
('admin',   CURDATE(), '07:55:00', NULL, 21.02780, 105.83420, 1, ''),
('ttbtram', CURDATE(), '08:15:00', NULL, 21.02740, 105.83390, 1, 'Đi muộn 15 phút - kẹt xe'),
('lhcuong', CURDATE(), '07:59:00', '17:10:00', 21.02830, 105.83480, 1, ''),
('vmduc',   CURDATE(), '07:45:00', '17:05:00', 21.02800, 105.83440, 1, '');

-- ============================================================
-- ĐƠN TỪ — 4 đơn PENDING để Admin duyệt
-- ============================================================
INSERT INTO leave_requests (username, start_date, end_date, reason, leave_type, status) VALUES
('pvkhai',  CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 3 DAY, 'Về quê giải quyết việc gia đình', 'NGHI_PHEP', 'PENDING'),
('ttbtram', CURDATE(), CURDATE(), 'Xin đi muộn 30 phút do đưa con đi học', 'DI_MUON', 'PENDING'),
('lhcuong', CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 6 DAY, 'Công tác tại chi nhánh Đà Nẵng', 'CONG_TAC', 'PENDING'),
('ptdung',  CURDATE() + INTERVAL 7 DAY, CURDATE() + INTERVAL 7 DAY, 'Khám sức khỏe định kỳ', 'NGHI_PHEP', 'PENDING'),
('vmduc',   CURDATE() - INTERVAL 5 DAY, CURDATE() - INTERVAL 5 DAY, 'Nghỉ phép năm đã được duyệt', 'NGHI_PHEP', 'APPROVED');

-- ============================================================
-- CA LÀM VIỆC — tuần này (Thứ 2 → Thứ 6)
-- ============================================================
SET @monday = CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY;

INSERT INTO work_shifts (username, shift_date, shift_type, note) VALUES
('pvkhai',  DATE_ADD(@monday, INTERVAL 0 DAY), 'SANG',  'Ca sáng tại văn phòng'),
('pvkhai',  DATE_ADD(@monday, INTERVAL 2 DAY), 'CHIEU', 'Họp nhóm dự án'),
('ttbtram', DATE_ADD(@monday, INTERVAL 1 DAY), 'SANG',  'Chăm sóc khách hàng'),
('ttbtram', DATE_ADD(@monday, INTERVAL 3 DAY), 'CHIEU', 'Tư vấn hợp đồng mới'),
('lhcuong', DATE_ADD(@monday, INTERVAL 0 DAY), 'CHIEU', 'Bảo trì hệ thống'),
('lhcuong', DATE_ADD(@monday, INTERVAL 4 DAY), 'SANG',  'Trực kỹ thuật'),
('ptdung',  DATE_ADD(@monday, INTERVAL 1 DAY), 'CHIEU', 'Phỏng vấn ứng viên'),
('ptdung',  DATE_ADD(@monday, INTERVAL 3 DAY), 'SANG',  'Họp nhân sự'),
('vmduc',   DATE_ADD(@monday, INTERVAL 2 DAY), 'SANG',  'Tiếp nhận yêu cầu khách hàng'),
('vmduc',   DATE_ADD(@monday, INTERVAL 4 DAY), 'CHIEU', 'Gọi lại khách hàng');

-- ============================================================
-- GHI CHÚ CÔNG VIỆC
-- ============================================================
INSERT INTO work_notes (username, note_date, content, plan) VALUES
('pvkhai',  CURDATE() - INTERVAL 1 DAY, 'Hoàn thành báo cáo tuần, kiểm tra dữ liệu chấm công tháng.', 'Chuẩn bị nội dung họp nhóm vào sáng mai.'),
('ttbtram', CURDATE() - INTERVAL 2 DAY, 'Hỗ trợ khách hàng qua điện thoại và email.', 'Theo dõi phản hồi của khách hàng trong ngày.'),
('lhcuong', CURDATE() - INTERVAL 3 DAY, 'Cập nhật tài liệu hướng dẫn sử dụng hệ thống.', 'Rà soát lại quy trình chấm công.'),
('ptdung',  CURDATE() - INTERVAL 1 DAY, 'Sàng lọc hồ sơ ứng viên cho vị trí kỹ thuật.', 'Sắp lịch phỏng vấn tuần sau.');

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- KIỂM TRA NHANH
-- ============================================================
SELECT username, full_name, role, department, phone FROM users ORDER BY role, username;
SELECT COUNT(*) AS chamcong_7_ngay FROM chamcong WHERE work_date >= CURDATE() - INTERVAL 6 DAY;
SELECT COUNT(*) AS don_pending FROM leave_requests WHERE status = 'PENDING';
