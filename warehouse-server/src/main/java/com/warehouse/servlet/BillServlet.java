package com.warehouse.servlet;

import com.warehouse.service.InoutService;
import com.warehouse.util.JsonUtil;
import com.warehouse.util.Result;

import javax.servlet.http.*;
import java.io.IOException;

/**
 * 单号接口：
 *   GET /api/bill/next?type=I&date=2026-10-09
 *   GET /api/bill/{billNo}   按单号查明细
 */
public class BillServlet extends HttpServlet {
    private final InoutService service = new InoutService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null) path = "";
            if (path.startsWith("/")) path = path.substring(1);
            if (path.endsWith("/"))   path = path.substring(0, path.length() - 1);

            if (path.equals("next")) {
                String type = req.getParameter("type");
                String date = req.getParameter("date");
                JsonUtil.write(resp, 200, service.nextBillNo(type, date));
            } else if (!path.isEmpty()) {
                JsonUtil.write(resp, 200, service.getBillDetail(path));
            } else {
                JsonUtil.write(resp, 400, Result.err("缺少路径参数"));
            }
        } catch (Exception e) {
            JsonUtil.write(resp, 500, Result.err("服务器异常：" + e.getMessage()));
        }
    }
}
