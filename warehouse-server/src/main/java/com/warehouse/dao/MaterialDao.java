package com.warehouse.dao;

import com.warehouse.util.DBUtil;

import java.sql.*;
import java.util.*;

public class MaterialDao {

    /** 模糊查询（代码/名称/规格），q 为空时返回全部。 */
    public List<Map<String, Object>> search(String q) throws Exception {
        String sql = "SELECT mat_code, mat_name, spec, unit, stock, remark " +
                     "FROM warehouse_material " +
                     "WHERE (? IS NULL OR mat_code LIKE CONCAT('%',?,'%') " +
                     "    OR mat_name LIKE CONCAT('%',?,'%') " +
                     "    OR spec     LIKE CONCAT('%',?,'%')) " +
                     "ORDER BY mat_code";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String kw = (q == null || q.trim().isEmpty()) ? null : q.trim();
            for (int i = 0; i < 4; i++) ps.setString(i + 1, kw);
            return rows(ps);
        }
    }

    public Map<String, Object> findByCode(String code) throws Exception {
        String sql = "SELECT mat_code, mat_name, spec, unit, stock, remark " +
                     "FROM warehouse_material WHERE mat_code=?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            List<Map<String, Object>> rs = rows(ps);
            return rs.isEmpty() ? null : rs.get(0);
        }
    }

    /** 客户端 matCode 为空时调用此方法生成下一个代码：M+yyyyMMdd+3位流水。 */
    public String nextCode() throws Exception {
        String prefix = "M" + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String sql = "SELECT MAX(mat_code) FROM warehouse_material WHERE mat_code LIKE CONCAT(?, '%')";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                int seq = 1;
                if (rs.next() && rs.getString(1) != null) {
                    seq = Integer.parseInt(rs.getString(1).substring(prefix.length())) + 1;
                }
                return prefix + String.format("%03d", seq);
            }
        }
    }

    public void insert(Map<String, Object> m) throws Exception {
        String sql = "INSERT INTO warehouse_material(mat_code,mat_name,spec,unit,stock,remark) " +
                     "VALUES(?,?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, (String) m.get("matCode"));
            ps.setString(2, (String) m.get("matName"));
            ps.setString(3, (String) m.get("spec"));
            ps.setString(4, (String) m.getOrDefault("unit", "个"));
            ps.setInt   (5, (Integer) m.getOrDefault("stock", 0));
            ps.setString(6, (String) m.get("remark"));
            ps.executeUpdate();
        }
    }

    public void update(String code, Map<String, Object> m) throws Exception {
        String sql = "UPDATE warehouse_material SET mat_name=?, spec=?, unit=?, stock=?, remark=? " +
                     "WHERE mat_code=?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, (String) m.get("matName"));
            ps.setString(2, (String) m.get("spec"));
            ps.setString(3, (String) m.getOrDefault("unit", "个"));
            ps.setInt   (4, (Integer) m.getOrDefault("stock", 0));
            ps.setString(5, (String) m.get("remark"));
            ps.setString(6, code);
            ps.executeUpdate();
        }
    }

    public void delete(String code) throws Exception {
        String sql = "DELETE FROM warehouse_material WHERE mat_code=?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        }
    }

    private List<Map<String, Object>> rows(PreparedStatement ps) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("matCode", rs.getString("mat_code"));
                m.put("matName", rs.getString("mat_name"));
                m.put("spec",    rs.getString("spec"));
                m.put("unit",    rs.getString("unit"));
                m.put("stock",   rs.getInt("stock"));
                m.put("remark",  rs.getString("remark"));
                out.add(m);
            }
        }
        return out;
    }
}
