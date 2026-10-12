package com.warehouse.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库工具：极简实现，每次获取一个新连接。
 * 报告里可写"可替换为 HikariCP / Druid 以提升并发性能"。
 */
public class DBUtil {
    private static String driver;
    private static String url;
    private static String user;
    private static String pwd;

    static {
        try (InputStream in = DBUtil.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            Properties p = new Properties();
            p.load(in);
            driver = p.getProperty("jdbc.driver");
            url    = p.getProperty("jdbc.url");
            user   = p.getProperty("jdbc.username");
            pwd    = p.getProperty("jdbc.password");
            Class.forName(driver);
        } catch (Exception e) {
            throw new RuntimeException("加载 db.properties 失败", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pwd);
    }

    public static void close(Connection c, Statement s, ResultSet r) {
        try { if (r != null) r.close(); } catch (SQLException ignored) {}
        try { if (s != null) s.close(); } catch (SQLException ignored) {}
        try { if (c != null) c.close(); } catch (SQLException ignored) {}
    }
}
