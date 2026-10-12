package com.warehouse.util;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** 统一设置请求/响应的字符集为 UTF-8（在 web.xml 中注册，避免与注解重复）。 */
public class CharsetFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        HttpServletResponse r = (HttpServletResponse) resp;
        r.setCharacterEncoding("UTF-8");
        r.setContentType("application/json;charset=UTF-8");
        // 允许跨域（仅本地调试用，生产环境收紧）
        r.setHeader("Access-Control-Allow-Origin", "*");
        r.setHeader("Access-Control-Allow-Headers", "Content-Type,X-User-Code");
        r.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        chain.doFilter(req, resp);
    }
}
