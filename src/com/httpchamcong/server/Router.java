package com.httpchamcong.server;

import com.httpchamcong.model.User;
import com.httpchamcong.server.controller.*;
import com.httpchamcong.util.JsonUtil;
import com.httpchamcong.util.SessionManager;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class Router {

    private final AuthController auth = new AuthController();
    private final ChamCongController chamCong = new ChamCongController();
    private final AdminController admin = new AdminController();
    private final UserController userCtrl = new UserController();
    private final LeaveController leaveCtrl = new LeaveController();
    private final WorkShiftController workShiftCtrl = new WorkShiftController();
    private final NoteController noteCtrl = new NoteController();  
    private final FileController fileCtrl = new FileController(); 
    private final DashboardController dashboardCtrl = new DashboardController();

    public HttpResponseBuilder handle(HttpRequestParser req) {
        String path = req.getPath();
        String method = req.getMethod();

        // RFC 7231: HEAD được xử lý như GET, nhưng ClientHandler sẽ không gửi body
        if ("HEAD".equals(method)) method = "GET";

        // RFC 7231: OPTIONS trả về các method được hỗ trợ
        if ("OPTIONS".equals(method)) {
            return new HttpResponseBuilder()
                .status(204)
                .header("Allow", "GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS")
                .body("");
        }

        // ============ AUTHORIZATION: ADMIN-ONLY API ============
        if (path.startsWith("/api/admin/")) {
            User current = SessionManager.getUser(req.getBearerToken());
            if (current == null) {
                return new HttpResponseBuilder().status(401)
                    .json("{\"error\":\"Chua dang nhap hoac token khong hop le\"}");
            }
            if (!"ADMIN".equalsIgnoreCase(current.getRole())) {
                return new HttpResponseBuilder().status(403)
                    .json("{\"error\":\"Khong co quyen truy cap (chi ADMIN)\"}");
            }
        }

        // ============ FILE DOWNLOAD SERVICE ============
        if (path.startsWith("/files/")) {
            String fileName = URLDecoder.decode(path.substring("/files/".length()), StandardCharsets.UTF_8);
            return fileCtrl.download(req, fileName);
        }

        // ============ WEB CLIENT (HTML/CSS/JS) ============
        // Mọi path không phải /api/... sẽ được thử phục vụ như file tĩnh.
        if (!path.startsWith("/api/")) {
            HttpResponseBuilder page = StaticFileHandler.serve(path, method);
            if (page != null) return page;
        }

        try {

            // GET/DELETE /api/leaves/{id}
            if (path.startsWith("/api/leaves/") && !path.equals("/api/leaves")) {
                String idStr = path.substring("/api/leaves/".length());
                if ("GET".equals(method)) return leaveCtrl.getById(req, idStr);
                if ("DELETE".equals(method)) return leaveCtrl.delete(req, idStr);
                return methodNotAllowed("GET, DELETE");
            }

            // PATCH/DELETE /api/admin/users/{username}
            if (path.startsWith("/api/admin/users/") && !path.equals("/api/admin/users")) {
                String username = path.substring("/api/admin/users/".length());
                if ("PATCH".equals(method)) return admin.patchUser(req, username);
                if ("DELETE".equals(method)) return admin.deleteUserByPath(req, username);
                return methodNotAllowed("PATCH, DELETE");
            }

            // ⭐ DELETE /api/notes/{id}
            if (path.startsWith("/api/notes/") && !path.equals("/api/notes")) {
                String idStr = path.substring("/api/notes/".length());
                if (idStr.matches("\\d+")) {
                    if ("DELETE".equals(method)) return noteCtrl.delete(req, idStr);
                    return methodNotAllowed("DELETE");
                }
            }

            // DELETE /api/work-shifts/{id} — chỉ khi id là SỐ
            if (path.startsWith("/api/work-shifts/")
                    && !path.equals("/api/work-shifts/list")
                    && !path.equals("/api/work-shifts/register")) {

                String idStr = path.substring("/api/work-shifts/".length());

                // ⭐ Chỉ xử lý nếu idStr là SỐ (không phải "list" hay "register")
                if (idStr.matches("\\d+")) {
                    if ("DELETE".equals(method)) return workShiftCtrl.delete(req, idStr);
                    return methodNotAllowed("DELETE");
                }
            }

            // ============ ROUTING THEO PATH + METHOD ============
            switch (path) {

                // AUTH
                case "/api/login":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return auth.login(req);

                case "/api/logout":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return handleLogout(req);

                case "/api/ping":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return handlePing();

                // CHẤM CÔNG
                case "/api/checkin":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return chamCong.checkIn(req);

                case "/api/checkout":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return chamCong.checkOut(req);

                case "/api/history":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return chamCong.history(req);

                case "/api/attendance/today":
                case "/api/today":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return chamCong.today(req);

                case "/api/export/csv":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return chamCong.exportCsv(req);

                // USER
                case "/api/register":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return userCtrl.register(req);

                case "/api/change-password":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return userCtrl.changePassword(req);

                case "/api/profile":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return userCtrl.profile(req);

                // NGHỈ PHÉP
                case "/api/leaves":
                    if ("GET".equals(method)) return leaveCtrl.all(req);
                    if ("POST".equals(method)) return leaveCtrl.request(req);
                    return methodNotAllowed("GET, POST");

                case "/api/leave-request":
                case "/api/leave-requests":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return leaveCtrl.request(req);

                case "/api/leave-list":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return leaveCtrl.list(req);

                case "/api/admin/leave/update":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return leaveCtrl.updateStatus(req);

                // ⭐ CA LÀM VIỆC — USER
                case "/api/work-shifts/register":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return workShiftCtrl.register(req);

                case "/api/work-shifts/list":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return workShiftCtrl.list(req);

                // ⭐ CA LÀM VIỆC — ADMIN
                case "/api/admin/work-shifts":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return workShiftCtrl.list(req);

                // ⭐ GHI CHÚ CÔNG VIỆC
                case "/api/notes/save":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return noteCtrl.save(req);

                case "/api/notes/my":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return noteCtrl.listMy(req);

                case "/api/notes/today":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return noteCtrl.today(req);

                case "/api/notes/all":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return noteCtrl.listAll(req);

                // ADMIN
                case "/api/admin/all":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return admin.getAll(req);

                case "/api/admin/users":
                    if ("GET".equals(method)) return admin.listUsers(req);
                    if ("POST".equals(method)) return admin.addUser(req);
                    return methodNotAllowed("GET, POST");

                case "/api/admin/user/add":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return admin.addUser(req);

                case "/api/admin/user/update":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return admin.updateUser(req);

                case "/api/admin/user/delete":
                    if (!"POST".equals(method)) return methodNotAllowed("POST");
                    return admin.deleteUser(req);

                case "/api/admin/leaves":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return leaveCtrl.all(req);

                // DASHBOARD + GIÁM SÁT
                case "/api/dashboard/stats":
                case "/api/admin/dashboard/stats":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return dashboardCtrl.stats(req);

                case "/api/admin/clients":
                case "/api/admin/monitor":
                    if (!"GET".equals(method)) return methodNotAllowed("GET");
                    return admin.monitor(req);

                // DUYỆT ĐƠN — API mới theo yêu cầu đồ án
                case "/api/leave/approve":
                    if (!"PUT".equals(method) && !"POST".equals(method))
                        return methodNotAllowed("PUT, POST");
                    return leaveCtrl.updateStatus(req);

                default:
                    return new HttpResponseBuilder().status(404)
                        .json("{\"error\":\"Endpoint khong ton tai: " + JsonUtil.escape(path) + "\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new HttpResponseBuilder().status(500)
                .json("{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    private HttpResponseBuilder methodNotAllowed(String allow) {
        return new HttpResponseBuilder()
            .status(405)
            .header("Allow", allow)
            .json("{\"error\":\"Method khong duoc phep. Chi chap nhan: " + allow + "\"}");
    }

    private HttpResponseBuilder handleLogout(HttpRequestParser req) {
        String tk = req.getBearerToken();
        if (tk != null) {
            User uu = SessionManager.getUser(tk);
            if (uu != null) ClientManager.setOffline(uu.getUsername());
            SessionManager.remove(tk);
        }
        return new HttpResponseBuilder().status(200)
            .json("{\"msg\":\"Da dang xuat\"}");
    }

    private HttpResponseBuilder handlePing() {
        return new HttpResponseBuilder().status(200)
            .json("{\"msg\":\"pong\"}");
    }
}