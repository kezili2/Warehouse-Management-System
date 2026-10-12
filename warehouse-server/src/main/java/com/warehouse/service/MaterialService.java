package com.warehouse.service;

import com.warehouse.dao.MaterialDao;
import com.warehouse.util.Result;

import java.util.List;
import java.util.Map;

/** 业务逻辑层：物料档案（子系统1）。 */
public class MaterialService {
    private final MaterialDao dao = new MaterialDao();

    public Result search(String q) throws Exception {
        return Result.ok(dao.search(q));
    }

    public Result findByCode(String code) throws Exception {
        Map<String, Object> m = dao.findByCode(code);
        return m == null ? Result.err("物料不存在") : Result.ok(m);
    }

    public Result insert(Map<String, Object> m) throws Exception {
        if (m.get("matName") == null || m.get("matName").toString().isEmpty())
            return Result.err("物料名称不能为空");
        if (m.get("matCode") == null || m.get("matCode").toString().isEmpty()) {
            m.put("matCode", dao.nextCode());
        } else {
            if (dao.findByCode((String) m.get("matCode")) != null)
                return Result.err("物料代码已存在");
        }
        dao.insert(m);
        return Result.ok(m);
    }

    public Result update(String code, Map<String, Object> m) throws Exception {
        if (dao.findByCode(code) == null) return Result.err("物料不存在");
        dao.update(code, m);
        return Result.ok("修改成功");
    }

    public Result delete(String code) throws Exception {
        dao.delete(code);
        return Result.ok("删除成功");
    }
}
