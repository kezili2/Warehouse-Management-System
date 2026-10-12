package com.warehouse.client.net;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** HTTP 客户端：基于 HttpURLConnection，无需第三方 HTTP 库。
 *  统一返回 {code, flag, msg, data} 解析后的 Map。 */
public class ApiClient {
    private static final ObjectMapper M = new ObjectMapper();
    private final String base;

    public ApiClient() { this(Session.get().getServerUrl()); }
    public ApiClient(String base) { this.base = base; }

    public Map<String, Object> login(String username, String password) throws Exception {
        return post("/api/login", Map.of("username", username, "password", password), null);
    }

    public List<Map<String, Object>> listUsers() throws Exception {
        return (List<Map<String, Object>>) get("/api/users", null).get("data");
    }

    public List<Map<String, Object>> searchMaterials(String q) throws Exception {
        return (List<Map<String, Object>>) get("/api/materials",
                q == null || q.isEmpty() ? null : Map.of("q", q)).get("data");
    }

    public Map<String, Object> saveMaterial(Map<String, Object> m) throws Exception {
        return post("/api/materials", m, null);
    }

    public Map<String, Object> updateMaterial(String code, Map<String, Object> m) throws Exception {
        return exchange("PUT", "/api/materials/" + code, m, null);
    }

    public Map<String, Object> deleteMaterial(String code) throws Exception {
        return exchange("DELETE", "/api/materials/" + code, null, null);
    }

    public String nextBillNo(String type, String date) throws Exception {
        Map<String, Object> r = get("/api/bill/next", Map.of("type", type, "date", date));
        return (String) ((Map<String, Object>) r.get("data")).get("billNo");
    }

    public Map<String, Object> inoutSingle(Map<String, Object> p) throws Exception {
        return post("/api/inout/single", p, Session.get().getUserCode());
    }

    public Map<String, Object> inoutMulti(Map<String, Object> p) throws Exception {
        return post("/api/inout/multi", p, Session.get().getUserCode());
    }

    public List<Map<String, Object>> searchRecords(Map<String, Object> q) throws Exception {
        return (List<Map<String, Object>>) get("/api/inout/records", q).get("data");
    }

    public Map<String, Object> getBillDetail(String billNo) throws Exception {
        return get("/api/bill/" + billNo, null);
    }

    // ---------- 底层 HTTP ----------
    private Map<String, Object> get(String path, Map<String, Object> query) throws Exception {
        return exchange("GET", path, null, query);
    }

    private Map<String, Object> post(String path, Object body, String userCode) throws Exception {
        return exchange("POST", path, body, null, userCode);
    }

    private Map<String, Object> exchange(String method, String path, Object body, Map<String, Object> query) throws Exception {
        return exchange(method, path, body, query, null);
    }

    private Map<String, Object> exchange(String method, String path, Object body,
                                         Map<String, Object> query, String userCode) throws Exception {
        StringBuilder url = new StringBuilder(base).append(path);
        if (query != null && !query.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (var e : query.entrySet()) {
                if (e.getValue() == null) continue;
                if (!first) url.append("&");
                url.append(e.getKey()).append("=")
                   .append(java.net.URLEncoder.encode(e.getValue().toString(), "UTF-8"));
                first = false;
            }
        }
        HttpURLConnection c = (HttpURLConnection) new URL(url.toString()).openConnection();
        c.setRequestMethod(method);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (userCode != null) c.setRequestProperty("X-User-Code", userCode);
        if (body != null) {
            c.setDoOutput(true);
            try (OutputStream os = c.getOutputStream()) {
                os.write(M.writeValueAsBytes(body));
            }
        }
        int status = c.getResponseCode();
        InputStream is = status >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = new String(readAll(is), StandardCharsets.UTF_8);
        if (text.isEmpty()) return Map.of("code", 0, "flag", "ERR", "msg", "空响应");
        @SuppressWarnings("unchecked")
        Map<String, Object> r = M.readValue(text, Map.class);
        return r;
    }

    private static byte[] readAll(InputStream is) throws IOException {
        if (is == null) return new byte[0];
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) >= 0) b.write(buf, 0, n);
        return b.toByteArray();
    }
}
