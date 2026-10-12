package com.warehouse.service;

import com.warehouse.dao.UserDao;
import com.warehouse.util.Result;

import java.util.List;
import java.util.Map;

/** 业务逻辑层：UserService（登录 + 用户列表）。 */
public class UserService {
    private final UserDao dao = new UserDao();

    public Result login(String username, String password) throws Exception {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return Result.err("用户名或密码不能为空");
        }
        Map<String, Object> u = dao.login(username, password);
        if (u == null) return Result.err("用户名或密码错误");
        return Result.ok(u);
    }

    public Result list() throws Exception {
        List<Map<String, Object>> data = dao.list();
        return Result.ok(data);
    }
}
