package com.warehouse.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/** 统一响应体：{code, flag, msg, data}。 */
public class Result {
    private static final ObjectMapper M = new ObjectMapper();

    public int    code;  // 1=成功，0 或负数=失败
    public String flag;  // OK / ERR
    public String msg;
    public Object data;

    public static Result ok(Object data) {
        Result r = new Result();
        r.code = 1; r.flag = "OK"; r.msg = "成功"; r.data = data;
        return r;
    }
    public static Result ok() { return ok(null); }

    public static Result err(String msg) {
        Result r = new Result();
        r.code = 0; r.flag = "ERR"; r.msg = msg;
        return r;
    }
    public static Result err(int code, String msg) {
        Result r = new Result();
        r.code = code; r.flag = "ERR"; r.msg = msg;
        return r;
    }

    /** 序列化为 JSON 字符串。内部消化异常（本类字段都是简单类型，实际不会失败）。 */
    public String toJson() {
        try {
            return M.writeValueAsString(this);
        } catch (Exception e) {
            // 理论上不可达；兜底返回一个固定 JSON，保证调用方无需处理检查异常
            return "{\"code\":0,\"flag\":\"ERR\",\"msg\":\"serialize error\"}";
        }
    }

    /** 解析请求体为 Map（不依赖具体 POJO）。 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse(String body) throws Exception {
        if (body == null || body.isEmpty()) return new LinkedHashMap<>();
        return M.readValue(body, Map.class);
    }
}
