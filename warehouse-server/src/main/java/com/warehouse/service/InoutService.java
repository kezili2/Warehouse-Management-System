package com.warehouse.service;

import com.warehouse.dao.InoutDao;
import com.warehouse.util.Result;

import java.util.List;
import java.util.Map;

/** 业务逻辑层：进出仓（子系统2/3）。 */
public class InoutService {
    private final InoutDao dao = new InoutDao();

    public Result single(Map<String, Object> p) throws Exception {
        String type = (String) p.get("ioType");
        if (!"I".equals(type) && !"O".equals(type)) return Result.err("ioType 必须是 I 或 O");
        if (p.get("billNo")  == null) return Result.err("billNo 不能为空");
        if (p.get("matCode") == null) return Result.err("matCode 不能为空");
        if (p.get("qty")     == null) return Result.err("qty 不能为空");
        if (p.get("opDate")  == null) return Result.err("opDate 不能为空");
        if (p.get("operator")== null) return Result.err("operator 不能为空");
        if (p.get("handler") == null) return Result.err("handler 不能为空");
        return dao.callSingle(type, p);
    }

    @SuppressWarnings("unchecked")
    public Result multi(Map<String, Object> p) throws Exception {
        String type = (String) p.get("ioType");
        if (!"I".equals(type) && !"O".equals(type)) return Result.err("ioType 必须是 I 或 O");
        if (p.get("billNo")  == null) return Result.err("billNo 不能为空");
        if (p.get("opDate")  == null) return Result.err("opDate 不能为空");
        if (p.get("operator")== null) return Result.err("operator 不能为空");
        if (p.get("handler") == null) return Result.err("handler 不能为空");
        List<Map<String, Object>> items = (List<Map<String, Object>>) p.get("items");
        if (items == null || items.isEmpty()) return Result.err("物料明细不能为空");
        return dao.callMulti(type, p, items);
    }

    public Result nextBillNo(String type, String date) throws Exception {
        if (type == null || date == null) return Result.err("type 和 date 不能为空");
        String billNo = dao.nextBillNo(type, java.sql.Date.valueOf(date));
        return Result.ok(java.util.Collections.singletonMap("billNo", billNo));
    }

    public Result searchRecords(Map<String, Object> q) throws Exception {
        return Result.ok(dao.searchRecords(q));
    }

    public Result getBillDetail(String billNo) throws Exception {
        Map<String, Object> m = dao.getBillDetail(billNo);
        return m == null ? Result.err("单据不存在") : Result.ok(m);
    }
}
