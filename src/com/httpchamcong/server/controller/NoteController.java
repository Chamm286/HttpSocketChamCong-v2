package com.httpchamcong.server.controller;

import com.httpchamcong.dao.WorkNoteDAO;
import com.httpchamcong.model.User;
import com.httpchamcong.model.WorkNote;
import com.httpchamcong.server.HttpRequestParser;
import com.httpchamcong.server.HttpResponseBuilder;
import com.httpchamcong.util.JsonUtil;
import com.httpchamcong.util.SessionManager;
import com.httpchamcong.util.ValidationUtil;

import java.util.List;

public class NoteController {

    public HttpResponseBuilder save(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        String date = JsonUtil.getString(req.getBody(), "noteDate");
        String content = JsonUtil.getString(req.getBody(), "content");
        String plan = JsonUtil.getString(req.getBody(), "plan");

        if (date == null || date.isEmpty() || content == null || content.isEmpty())
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"Thieu ngay hoac noi dung\"}");

        if (!ValidationUtil.isIsoDate(date))
            return new HttpResponseBuilder().status(400)
                .json("{\"error\":\"noteDate phai theo dinh dang yyyy-MM-dd\"}");

        WorkNote note = new WorkNote(u.getUsername(), date, content, plan);
        boolean ok = WorkNoteDAO.saveOrUpdate(note);

        return ok
            ? new HttpResponseBuilder().status(200).json("{\"msg\":\"Da luu ghi chu\"}")
            : new HttpResponseBuilder().status(500).json("{\"error\":\"Khong the luu\"}");
    }

    public HttpResponseBuilder listMy(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        List<WorkNote> list = WorkNoteDAO.findByUser(u.getUsername());
        return buildListResponse(list);
    }

    public HttpResponseBuilder today(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        String today = java.time.LocalDate.now().toString();
        WorkNote note = WorkNoteDAO.findByUserAndDate(u.getUsername(), today);

        if (note == null)
            return new HttpResponseBuilder().status(200).json("{\"data\":null}");

        return new HttpResponseBuilder().status(200)
            .json("{\"data\":" + note.toJson() + "}");
    }

    public HttpResponseBuilder listAll(HttpRequestParser req) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null || !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403)
                .json("{\"error\":\"Khong co quyen\"}");

        List<WorkNote> list = WorkNoteDAO.loadAll();
        return buildListResponse(list);
    }

    public HttpResponseBuilder delete(HttpRequestParser req, String idStr) {
        String token = req.getBearerToken();
        User u = SessionManager.getUser(token);
        if (u == null)
            return new HttpResponseBuilder().status(401)
                .json("{\"error\":\"Chua dang nhap\"}");

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (Exception e) {
            return new HttpResponseBuilder().status(400).json("{\"error\":\"id khong hop le\"}");
        }

        WorkNote note = WorkNoteDAO.findById(id);
        if (note == null)
            return new HttpResponseBuilder().status(404).json("{\"error\":\"Khong tim thay\"}");

        if (!note.getUsername().equals(u.getUsername()) && !"ADMIN".equals(u.getRole()))
            return new HttpResponseBuilder().status(403)
                .json("{\"error\":\"Chi duoc xoa ghi chu cua minh\"}");

        boolean ok = WorkNoteDAO.deleteById(id);
        return ok
            ? new HttpResponseBuilder().status(204).body("")
            : new HttpResponseBuilder().status(404).json("{\"error\":\"Khong tim thay\"}");
    }

    private HttpResponseBuilder buildListResponse(List<WorkNote> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return new HttpResponseBuilder().status(200)
            .json("{\"total\":" + list.size() + ",\"data\":" + sb + "}");
    }
}
