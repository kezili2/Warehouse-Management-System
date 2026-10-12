package com.warehouse.servlet;

import com.warehouse.service.UserService;
import com.warehouse.util.JsonUtil;
import com.warehouse.util.Result;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

/** POST /api/login */
public class LoginServlet extends HttpServlet {
    private final UserService service = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String body = readBody(req);
            java.util.Map<String, Object> m = Result.parse(body);
            String username = (String) m.get("username");
            String password = (String) m.get("password");
            Result r = service.login(username, password);
            JsonUtil.write(resp, 200, r);
        } catch (Exception e) {
            JsonUtil.write(resp, 500, Result.err("服务器异常：" + e.getMessage()));
        }
    }

    static String readBody(HttpServletRequest req) throws IOException {
        java.io.BufferedReader br = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }
}
