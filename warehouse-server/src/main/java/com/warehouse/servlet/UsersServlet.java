package com.warehouse.servlet;

import com.warehouse.service.UserService;
import com.warehouse.util.JsonUtil;
import com.warehouse.util.Result;

import javax.servlet.http.*;
import java.io.IOException;

/** GET /api/users */
public class UsersServlet extends HttpServlet {
    private final UserService service = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonUtil.write(resp, 200, service.list());
        } catch (Exception e) {
            JsonUtil.write(resp, 500, Result.err("服务器异常：" + e.getMessage()));
        }
    }
}
