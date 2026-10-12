package com.warehouse.servlet;

import com.warehouse.service.MaterialService;
import com.warehouse.util.JsonUtil;
import com.warehouse.util.Result;

import javax.servlet.http.*;
import java.io.IOException;

/**
 * 物料接口：
 *   GET    /api/materials?q=keyword
 *   GET    /api/materials/{code}
 *   POST   /api/materials
 *   PUT    /api/materials/{code}
 *   DELETE /api/materials/{code}
 */
public class MaterialsServlet extends HttpServlet {
    private final MaterialService service = new MaterialService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = trimPath(req.getPathInfo());
            if (path.isEmpty()) {
                JsonUtil.write(resp, 200, service.search(req.getParameter("q")));
            } else {
                JsonUtil.write(resp, 200, service.findByCode(path));
            }
        } catch (Exception e) {
            JsonUtil.write(resp, 500, Result.err("服务器异常：" + e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            java.util.Map<String, Object> m = Result.parse(LoginServlet.readBody(req));
            JsonUtil.write(resp, 200, service.insert(m));
        } catch (Exception e) {
            JsonUtil.write(resp, 500, Result.err("服务器异常：" + e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String code = trimPath(req.getPathInfo());
            java.util.Map<String, Object> m = Result.parse(LoginServlet.readBody(req));
            JsonUtil.write(resp, 200, service.update(code, m));
        } catch (Exception e) {
            JsonUtil.write(resp, 500, Result.err("服务器异常：" + e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String code = trimPath(req.getPathInfo());
            JsonUtil.write(resp, 200, service.delete(code));
        } catch (Exception e) {
            JsonUtil.write(resp, 500, Result.err("服务器异常：" + e.getMessage()));
        }
    }

    private static String trimPath(String p) {
        if (p == null) return "";
        if (p.startsWith("/")) p = p.substring(1);
        if (p.endsWith("/"))   p = p.substring(0, p.length() - 1);
        return p;
    }
}
