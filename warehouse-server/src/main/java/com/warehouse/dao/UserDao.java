package com.warehouse.dao;

import com.warehouse.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 人员档案 / 登录 DAO */
public class UserDao {

    /** 根据用户名+密码查询用户（含真实姓名与家庭住址）。 */
    public Map<String, Object> login(String username, String password) throws Exception {
        String sql = "SELECT user_code, real_name, address FROM warehouse_user " +
                     "WHERE username=? AND password=SHA2(?,256)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("userCode", rs.getString(1));
                    m.put("realName", rs.getString(2));
                    m.put("address",  rs.getString(3));
                    return m;
                }
                return null;
            }
        }
    }

    /** 全部用户（用于"经手人"下拉）。 */
    public List<Map<String, Object>> list() throws Exception {
        String sql = "SELECT user_code, real_name FROM warehouse_user ORDER BY user_code";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("userCode", rs.getString(1));
                m.put("realName", rs.getString(2));
                out.add(m);
            }
        }
        return out;
    }
}
