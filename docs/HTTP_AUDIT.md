# Báo cáo kiểm tra HTTP/1.1 & đối chiếu chức năng

> Phạm vi: toàn bộ server HTTP tự viết trên `ServerSocket`/`Socket` TCP thuần Java trong
> `src/com/httpchamcong/server/`, client tự build request trong
> `src/com/httpchamcong/client/SocketHttpClient.java`, và luồng chấm công trên giao diện JavaFX.
>
> Thời điểm kiểm tra: **2026-11-07** — Môi trường: Java 17+, MariaDB 10.4, Windows.

---

## 1. Kết quả kiểm thử tổng hợp

Bộ test raw-socket (`F:\Temp\opencode\http-test.ps1`) gửi request HTTP thủ công bằng
`TcpClient` (không dùng thư viện HTTP nào) tới server đang chạy ở `127.0.0.1:2020`.

```
RESULT: 22 / 22 passed
```

| # | Trường hợp | Kỳ vọng | Kết quả |
|---|-----------|---------|---------|
| 1 | `GET /api/ping` | `200 OK` | ✅ |
| 2 | Header `Server` | `JavaChamCongServer/1.1` | ✅ |
| 3 | Header `Date` | RFC 1123 IMF-fixdate (`Sat, 07 Nov 2026 16:05:01 GMT`) | ✅ |
| 4 | `HEAD /api/ping` | `200`, **không body** | ✅ |
| 5 | `OPTIONS /api/ping` | `204`, không body, không `Content-Length` | ✅ |
| 6 | `POST /api/login` đúng | `200` + token | ✅ |
| 7 | `POST /api/login` sai mật khẩu | `401 Unauthorized` | ✅ |
| 8 | `GET /api/attendance/today` trước chấm công | `NOT_CHECKED_IN` | ✅ |
| 9 | `POST /api/checkin` lần 1 | `201 Created` | ✅ |
| 10 | `POST /api/checkin` lần 2 | `400 Bad Request` | ✅ |
| 11 | `GET /api/attendance/today` sau check-in | `WORKING` | ✅ |
| 12 | `POST /api/checkout` | `200 OK` | ✅ |
| 13 | `GET /api/attendance/today` sau check-out | `COMPLETED` | ✅ |
| 14 | `GET /api/history?page=1&limit=2` | `200` + `total` | ✅ |
| 15 | `POST /api/history` (sai method) | `405 Method Not Allowed` + `Allow: GET` | ✅ |
| 16 | `GET /api/khong-ton-tai` | `404 Not Found` | ✅ |
| 17 | Thiếu token | `401 Unauthorized` | ✅ |
| 18 | `GET ... HTTP/1.0` | `505 HTTP Version Not Supported` | ✅ |
| 19 | `HTTP/1.1` thiếu `Host` | `400 Bad Request` | ✅ |
| 20 | Request line sai định dạng | `400 Bad Request` | ✅ |
| 21 | `GET /api/admin/all` với token ADMIN | `200 OK` | ✅ |
| 22 | `GET /api/admin/all` với token USER | `403 Forbidden` | ✅ |

---

## 2. Ma trận tuân thủ HTTP/1.1

Tham chiếu: RFC 7230 (message syntax/routing), RFC 7231 (semantics), RFC 7232/7235.

| Hạng mục | Yêu cầu chuẩn | Hiện trạng | Đánh giá |
|----------|---------------|------------|----------|
| Request line | `method SP request-target SP HTTP-version` | Parse đúng 3 phần, validate method token | ✅ |
| HTTP version | Chỉ hỗ trợ `HTTP/1.1` | Version khác → `505` | ✅ |
| `Host` header | Bắt buộc với HTTP/1.1 | Thiếu/rỗng → `400` | ✅ |
| Header syntax | `name: value`, không obs-fold | Tên header validate theo `tchar`; dòng bắt đầu bằng SP/HTAB → `400` | ✅ |
| Header name | Không phân biệt hoa/thường | `getHeader()` so sánh `equalsIgnoreCase` | ✅ |
| `Content-Length` | Số byte, không âm, không trùng lệch | Đọc body theo **byte[]** UTF-8, trùng lệch → `400` | ✅ |
| Body | Đọc đủ `Content-Length` | Thiếu byte → `400` | ✅ |
| Giới hạn kích thước | Chống request quá lớn | Header 64KB → `431`, body 1MB → `413` | ✅ |
| `Transfer-Encoding` | `chunked` là tuỳ chọn | Chưa hỗ trợ → `501` (báo lỗi rõ ràng) | ⚠️ Có giới hạn |
| Status line | `HTTP/1.1 code reason-phrase` | Map đầy đủ reason phrase cho các mã dùng | ✅ |
| `Date` header | RFC 1123 / IMF-fixdate, GMT | `DateTimeFormatter` + `Locale.ENGLISH` + `UTC` | ✅ |
| `Content-Length` response | Đúng số byte UTF-8 | Tính từ `body.getBytes(UTF_8)` | ✅ |
| `204`/`304`/`1xx` | Không body, không `Content-Length` | Tự loại body + header | ✅ |
| `HEAD` | Header như GET, không body | Router map HEAD→GET, `ClientHandler` chặn ghi body | ✅ |
| `OPTIONS` | Trả methods được hỗ trợ | `204` + `Allow` | ✅ |
| `405` | Kèm header `Allow` | Có `Allow` theo từng route | ✅ |
| Mã lỗi parse | Trả đúng 4xx/5xx thay vì đóng socket | `HttpParseException` mang `statusCode` | ✅ |
| `Connection` | HTTP/1.1 mặc định keep-alive | Luôn `Connection: close` (mỗi request 1 kết nối) | ⚠️ Có giới hạn |
| TLS | HTTPS là tuỳ chọn | Không có TLS (đồ án dùng TCP thuần) | ⚠️ Có giới hạn |
| `Content-Type` request | Nên kiểm tra với JSON | Server parse JSON trực tiếp, không bắt buộc header | ⚠️ Chấp nhận được |

**Kết luận:** Server đáp ứng các yêu cầu cốt lõi của HTTP/1.1 để chấm đồ án Lập trình mạng
(request line, Host, version, header/body framing, status code, Date, HEAD/OPTIONS/405).
Các mục ⚠️ là giới hạn có chủ đích, được ghi rõ ở mục 4.

---

## 3. Các lỗi đã sửa trong đợt kiểm tra này

| Lỗi trước đây | Hậu quả | Cách sửa |
|---------------|---------|----------|
| `Date` dùng `new Date().toString()` | Sai định dạng RFC (không GMT chuẩn) | `HttpResponseBuilder` dùng IMF-fixdate UTC |
| Không kiểm tra `Host` | Request HTTP/1.1 thiếu Host vẫn được nhận | `HttpRequestParser` trả `400` |
| Chấp nhận mọi version | `HTTP/1.0`, rác… vẫn xử lý | Chỉ nhận `HTTP/1.1`, còn lại `505` |
| Lỗi parse làm rơi kết nối | Client nhận EOF thay vì lỗi HTTP | `HttpParseException(status, msg)` → response lỗi chuẩn |
| `HEAD` bị coi là method lạ | `405` sai | Map HEAD→GET, không ghi body |
| Không hỗ trợ `OPTIONS` | `405` sai | `204` + `Allow` |
| `405` reason phrase sai/thiếu `Allow` | Client khó chẩn đoán | Reason phrase chuẩn + `Allow` từng route |
| `204` vẫn gửi `Content-Length: 0` | Không đúng khuyến nghị | Loại body + `Content-Length` + `Content-Type` |
| Body đọc theo char | Lỗi với tiếng Việt UTF-8 nhiều byte | Đọc `byte[]` theo `Content-Length` |
| JSON không escape | Chuỗi có `"`/`\` làm hỏng JSON | `JsonUtil` escape đầy đủ |
| Server không có `main()` headless | Không chạy được bằng CLI/CI | Thêm `HttpSocketServer.main()` |
| DB hard-code 1 port | Máy XAMPP dùng 3306 bị fail | Thử 3307 → 3306, override qua `-D`/env |
| Client hard-code IP LAN | Máy khác không chạy được | Mặc định `localhost`, override qua `-D`/env |

---

## 4. Hạn chế còn lại (có chủ đích)

1. **Không keep-alive** — server trả `Connection: close` và đóng socket sau mỗi request.
   Đúng semantics HTTP/1.1 nhưng không tối ưu; đủ cho đồ án và dễ lập trình.
2. **Không hỗ trợ `Transfer-Encoding: chunked`** — trả `501`. Client hiện luôn gửi
   `Content-Length`, nên không ảnh hưởng luồng thực tế.
3. **Không TLS/HTTPS** — dữ liệu truyền thô trên TCP; phù hợp yêu cầu "Socket TCP/IP thuần".
4. **Mật khẩu plaintext trong DB** — cần băm (BCrypt/PBKDF2) nếu triển khai thật.
5. **Session token trong bộ nhớ, không hết hạn** — mất khi restart server; nên thêm TTL.
6. **Không rate-limit** — chống brute-force là hướng phát triển thêm.
7. **Không có `ETag`/`If-None-Match`** — chưa cần cho API JSON nhỏ.

---

## 5. Đối chiếu chức năng với tên đề tài

Đề tài: **"Hệ thống Chấm công trực tuyến"** — trọng tâm là chấm công vào/ra, lịch sử,
thống kê và quản trị; tất cả qua HTTP/1.1 trên Socket TCP/IP.

| Chức năng | Endpoint / UI | Khớp đề tài |
|-----------|---------------|-------------|
| Đăng nhập / đăng xuất | `POST /api/login`, `POST /api/logout` | ✅ |
| Web client (HTML/CSS/JS) | `GET /`, `GET /assets/*` — `StaticFileHandler` | ✅ |
| Đăng ký tài khoản | `POST /api/register` | ✅ |
| **Chấm công vào** | `POST /api/checkin` | ✅ |
| **Chấm công ra** | `POST /api/checkout` | ✅ |
| **Trạng thái chấm công hôm nay** | `GET /api/attendance/today` | ✅ (bổ sung) |
| Lịch sử chấm công | `GET /api/history` | ✅ |
| Thống kê cá nhân | tổng hợp từ history | ✅ |
| Quản trị toàn bộ chấm công | `GET /api/admin/all` | ✅ |
| Xuất CSV | `GET /api/export/csv` | ✅ |
| Xin nghỉ phép | `/api/leave-request`, `/api/leave-list` | ✅ |
| Ca làm việc | `/api/work-shifts/*` | ✅ |
| Ghi chú công việc | `/api/notes/*` | ✅ |
| Quản lý người dùng (ADMIN) | `/api/admin/users`, `/api/admin/user/*` | ✅ |
| Kiểm tra server | `GET /api/ping` | ✅ |

### Luồng chấm công đã được hợp nhất

Trước đây giao diện có hai trang/2 nút độc lập "Chấm công vào" và "Chấm công ra" →
người dùng có thể bấm sai thứ tự, không biết mình đang ở trạng thái nào.

Nay chỉ còn **một mục "Chấm công"** với máy trạng thái:

```
NOT_CHECKED_IN ──[CHECK-IN]──▶ WORKING ──[CHECK-OUT]──▶ COMPLETED
       ▲                                                     │
       └────────────────── ngày làm việc mới ────────────────┘
```

- Trang tải `GET /api/attendance/today` khi mở.
- Chỉ hiển thị **một nút hành động chính** đúng với trạng thái hiện tại.
- Có hộp thoại xác nhận trước khi ghi nhận.
- Server là nguồn thời gian duy nhất (`serverTime`), client không tự sửa giờ.
- Chặn check-in lần 2 (`400`) và check-out khi chưa check-in (`400`).
- Dashboard Trang chủ lấy trạng thái thật thay vì text tĩnh.

---

## 6. Cách chạy lại kiểm thử

```powershell
# 1) Bảo đảm server đang chạy
java -cp "bin;lib/mysql-connector-j-*.jar" com.httpchamcong.server.HttpSocketServer

# 2) Reset dữ liệu chấm công hôm nay của tài khoản test (nếu cần)
& 'C:\xampp\mysql\bin\mysql.exe' -h 127.0.0.1 -P 3307 -u root --skip-password `
  -e "USE chamcong_db; DELETE FROM chamcong WHERE username='nhanvien1' AND work_date=CURDATE();"

# 3) Chạy bộ test raw socket
powershell -NoProfile -ExecutionPolicy Bypass -File F:\Temp\opencode\http-test.ps1
```

Kỳ vọng: `RESULT: 22 / 22 passed`.
