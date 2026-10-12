package com.warehouse.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/** Servlet 公共方法：写 JSON 响应。内部消化 IOException，调用方无需再捕获。 */
public class JsonUtil {
    public static void write(HttpServletResponse resp, int status, Result r) {
        resp.setStatus(status);
        try (PrintWriter w = resp.getWriter()) {
            w.print(r.toJson());
        } catch (IOException e) {
            // 响应已提交或客户端断开，无法再写错误信息，仅记录日志
            e.printStackTrace();
        }
    }
}
