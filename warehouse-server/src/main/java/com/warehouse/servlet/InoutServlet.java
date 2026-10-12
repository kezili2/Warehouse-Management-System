package com.warehouse.servlet;

import com.warehouse.service.InoutService;
import com.warehouse.util.JsonUtil;
import com.warehouse.util.Result;

import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;

/**
 * 进出仓接口：
 *   POST /api/inout/single  单物料
 *   POST /api/inout/multi   多物料
 *   GET  /api/inout/records?dateFrom=&dateTo=&matCode=&operator=&remark=
 *
 * 所有受保护接口要求 HTTP 头 X-User-Code 与 body.operator 一致。
 */
public class InoutServlet extends HttpServlet {
    private final InoutService service = new InoutService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null) path = "";
            if (path.startsWith("/")) path = path.substring(1);

            Map<String, Object> m = Result.parse(LoginServlet.readBody(req));

            // 校验 X-User-Code 与 body.operator 一致
            String headerUser = req.getHeader("X-User-Code");
            String bodyUser   = (String) m.get("operator");
            if (headerUser == null || bodyUser == null || !headerUser.equals(bodyUser)) {
                JsonUtil.write(resp, 401, Result.err("操作人员与登录用户不一致"));
                return;
            }

            if (path.equals("single")) {
                JsonUtil.write(resp, 200, service.single(m));
            } else if (path.equals("multi")) {
                JsonUtil.write(resp, 200, service.multi(m));
            } else {
                JsonUtil.write(resp, 400, Result.err("未知子路径：" + path));
            }
        } catch (Exception e) {
            JsonUtil.write(resp, 500, Result.err("服务器异常：" + e.getMessage()));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null) path = "";
            if (path.startsWith("/")) path = path.substring(1);

            if (path.equals("records")) {
                Map<String, Object> q = new java.util.HashMap<>();
                q.put("dateFrom", req.getParameter("dateFrom"));
                q.put("dateTo",   req.getParameter("dateTo"));
                q.put("matCode",  req.getParameter("matCode"));
                q.put("operator", req.getParameter("operator"));
                q.put("remark",   req.getParameter("remark"));
                JsonUtil.write(resp, 200, service.searchRecords(q));
            } else {
                JsonUtil.write(resp, 400, Result.err("未知子路径：" + path));
            }
        } catch (Exception e) {
            JsonUtil.write(resp, 500, Result.err("服务器异常：" + e.getMessage()));
        }
    }
}
