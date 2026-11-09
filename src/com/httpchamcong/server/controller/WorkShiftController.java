package com.httpchamcong.server.controller;

import com.httpchamcong.dao.WorkShiftDAO;
import com.httpchamcong.model.User;
import com.httpchamcong.model.WorkShift;
import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;
import com.httpchamcong.util.JsonUtil;
import com.httpchamcong.util.SessionManager;
import com.httpchamcong.util.ValidationUtil;

import java.util.List;

public class WorkShiftController {

    /** POST /api/work-shifts/register */
    public HttpResponseBuilder register(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        String shiftDate = JsonUtil.getString(req.getBody(), "shiftDate");
        String shiftType = JsonUtil.getString(req.getBody(), "shiftType");
        String note = JsonUtil.getString(req.getBody(), "note");

        if (shiftDate == null || shiftType == null)
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Thieu shiftDate hoac shiftType\"}");

        if (!ValidationUtil.isIsoDate(shiftDate))
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"shiftDate phai theo dinh dang yyyy-MM-dd\"}");

        if (!shiftType.equals("SANG") && !shiftType.equals("CHIEU") && !shiftType.equals("TOI"))
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"shiftType phai la SANG, CHIEU, hoac TOI\"}");

        if (WorkShiftDAO.exists(u.getUsername(), shiftDate, shiftType))
            return new HttpResponseBuilder().status(409)
                .json("{\"error\":\"Ban da dang ky ca " + shiftType + " ngay " + shiftDate + "\"}");

        WorkShift ws = new WorkShift(u.getUsername(), shiftDate, shiftType, note);
        boolean ok = WorkShiftDAO.insert(ws);

        if (!ok)
            return new HttpResponseBuilder().status(500)
                .json("{\"error\":\"Khong the dang ky ca\"}");

        return new HttpResponseBuilder().status(201)
            .json("{\"msg\":\"Dang ky ca thanh cong\"}");
    }

    /** GET /api/work-shifts/list */
    public HttpResponseBuilder list(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        List<WorkShift> list;
        if ("ADMIN".equals(u.getRole())) {
            list = WorkShiftDAO.loadAll();
        } else {
            list = WorkShiftDAO.findByUser(u.getUsername());
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");

        return new HttpResponseBuilder().status(200)
            .json("{\"total\":" + list.size() + ",\"data\":" + sb + "}");
    }

    /** DELETE /api/work-shifts/{id} */
    public HttpResponseBuilder delete(HttpRequestParser req, String idStr) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        int id;
        try { id = Integer.parseInt(idStr); }
        catch (Exception e) {
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"id khong hop le\"}");
        }

        WorkShift shift = WorkShiftDAO.findById(id);
        if (shift == null)
            return new HttpResponseBuilder().status(404)
                .json("{\"error\":\"Khong tim thay ca\"}");

        if (!shift.getUsername().equals(u.getUsername()) && !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403)
                .json("{\"error\":\"Chi duoc xoa ca cua minh\"}");

        boolean ok = WorkShiftDAO.deleteById(id);
        return ok
            ? new HttpResponseBuilder().status(204).body("")
            : new HttpResponseBuilder().status(404).json("{\"error\":\"Khong tim thay ca\"}");
    }
}