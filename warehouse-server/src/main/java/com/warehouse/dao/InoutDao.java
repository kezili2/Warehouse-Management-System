package com.warehouse.dao;

import com.warehouse.util.DBUtil;
import com.warehouse.util.Result;

import java.sql.*;
import java.util.*;

public class InoutDao {

    /** 单物料进/出仓。type: I=进 O=出。 */
    public Result callSingle(String type, Map<String, Object> p) throws Exception {
        String sp = "I".equals(type) ? "CALL warehouse_sp_in(?,?,?,?,?,?,?)"
                                     : "CALL warehouse_sp_out(?,?,?,?,?,?,?)";
        try (Connection c = DBUtil.getConnection();
             CallableStatement cs = c.prepareCall(sp)) {
            cs.setString(1, (String) p.get("billNo"));
            cs.setString(2, (String) p.get("matCode"));
            cs.setInt   (3, (Integer) p.get("qty"));
            cs.setDate  (4, java.sql.Date.valueOf((String) p.get("opDate")));
            cs.setString(5, (String) p.get("operator"));
            cs.setString(6, (String) p.get("handler"));
            cs.setString(7, (String) p.get("remark"));
            return readProcResult(cs);
        }
    }

    /** 多物料进/出仓。type: I=进 O=出。items 为 List<Map{ matCode, qty }>。 */
    public Result callMulti(String type, Map<String, Object> p, List<Map<String, Object>> items) throws Exception {
        String json = toJsonArray(items);
        try (Connection c = DBUtil.getConnection();
             CallableStatement cs = c.prepareCall(
                 "CALL warehouse_sp_inout_multi(?,?,?,?,?,?,?)")) {
            cs.setString(1, (String) p.get("billNo"));
            cs.setString(2, type);
            cs.setString(3, json);
            cs.setDate  (4, java.sql.Date.valueOf((String) p.get("opDate")));
            cs.setString(5, (String) p.get("operator"));
            cs.setString(6, (String) p.get("handler"));
            cs.setString(7, (String) p.get("remark"));
            return readProcResult(cs);
        }
    }

    /** 生成新单号。type: I / O / MI / MO。 */
    public String nextBillNo(String type, java.sql.Date date) throws Exception {
        String prefix;
        switch (type) {
            case "I":  prefix = "I";  break;
            case "O":  prefix = "O";  break;
            case "MI": prefix = "MI"; break;
            case "MO": prefix = "MO"; break;
            default: throw new IllegalArgumentException("type 仅支持 I/O/MI/MO");
        }
        String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(date);
        String like = prefix + dateStr + "%";
        String sql = "SELECT MAX(bill_no) FROM warehouse_inout WHERE bill_no LIKE ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            try (ResultSet rs = ps.executeQuery()) {
                int seq = 1;
                if (rs.next() && rs.getString(1) != null) {
                    seq = Integer.parseInt(rs.getString(1).substring(prefix.length() + dateStr.length())) + 1;
                }
                return prefix + dateStr + String.format("%03d", seq);
            }
        }
    }

    /** 进出仓记录查询（所有条件可选）。 */
    public List<Map<String, Object>> searchRecords(Map<String, Object> q) throws Exception {
        StringBuilder sql = new StringBuilder(
            "SELECT bill_no, io_type, mat_code, qty, op_date, operator, handler, remark " +
            "FROM warehouse_inout WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (notEmpty(q.get("dateFrom"))) { sql.append(" AND op_date >= ?");   args.add(java.sql.Date.valueOf((String) q.get("dateFrom"))); }
        if (notEmpty(q.get("dateTo")))   { sql.append(" AND op_date <= ?");   args.add(java.sql.Date.valueOf((String) q.get("dateTo"))); }
        if (notEmpty(q.get("matCode")))  { sql.append(" AND mat_code = ?");   args.add(q.get("matCode")); }
        if (notEmpty(q.get("operator"))) { sql.append(" AND operator = ?");   args.add(q.get("operator")); }
        if (notEmpty(q.get("remark")))   { sql.append(" AND remark LIKE ?");  args.add("%" + q.get("remark") + "%"); }
        sql.append(" ORDER BY op_date DESC, bill_no, mat_code");

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
            return recordRows(ps);
        }
    }

    /** 按单号查明细（含单头 + 物料列表）。 */
    public Map<String, Object> getBillDetail(String billNo) throws Exception {
        String headSql = "SELECT DISTINCT bill_no, io_type, op_date, operator, handler, remark " +
                         "FROM warehouse_inout WHERE bill_no=?";
        String itemsSql = "SELECT i.mat_code, m.mat_name, i.qty " +
                          "FROM warehouse_inout i LEFT JOIN warehouse_material m USING(mat_code) " +
                          "WHERE i.bill_no=? ORDER BY i.mat_code";
        try (Connection c = DBUtil.getConnection()) {
            Map<String, Object> out = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement(headSql)) {
                ps.setString(1, billNo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        out.put("billNo",   rs.getString(1));
                        out.put("ioType",   rs.getString(2));
                        out.put("opDate",   rs.getDate(3).toString());
                        out.put("operator", rs.getString(4));
                        out.put("handler",  rs.getString(5));
                        out.put("remark",   rs.getString(6));
                    } else {
                        return null;
                    }
                }
            }
            List<Map<String, Object>> items = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(itemsSql)) {
                ps.setString(1, billNo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("matCode", rs.getString(1));
                        m.put("matName", rs.getString(2));
                        m.put("qty",     rs.getInt(3));
                        items.add(m);
                    }
                }
            }
            out.put("details", items);
            return out;
        }
    }

    // ---------- 私有辅助 ----------
    private static boolean notEmpty(Object o) { return o != null && !o.toString().isEmpty(); }

    private static Result readProcResult(CallableStatement cs) throws SQLException {
        try (ResultSet rs = cs.executeQuery()) {
            if (rs.next()) {
                int code = rs.getInt(1);
                String flag = rs.getString(2);
                String msg  = rs.getString(3);
                return "OK".equals(flag) ? Result.ok(msg) : Result.err(code, msg);
            }
            return Result.err("存储过程无返回结果");
        } catch (SQLException e) {
            // 存储过程 SIGNAL 抛出的异常，把 message_text 透传给前端
            String m = e.getMessage();
            if (m != null && m.contains("\n")) m = m.substring(m.lastIndexOf("\n") + 1).trim();
            return Result.err(m == null ? "数据库异常" : m);
        }
    }

    private static String toJsonArray(List<Map<String, Object>> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, Object> m = items.get(i);
            sb.append("{\"matCode\":\"").append(m.get("matCode"))
              .append("\",\"qty\":").append(m.get("qty")).append("}");
        }
        return sb.append("]").toString();
    }

    private static List<Map<String, Object>> recordRows(PreparedStatement ps) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("billNo",  rs.getString(1));
                m.put("ioType",  rs.getString(2));
                m.put("matCode", rs.getString(3));
                m.put("qty",     rs.getInt(4));
                m.put("opDate",  rs.getDate(5).toString());
                m.put("operator",rs.getString(6));
                m.put("handler", rs.getString(7));
                m.put("remark",  rs.getString(8));
                out.add(m);
            }
        }
        return out;
    }
}
