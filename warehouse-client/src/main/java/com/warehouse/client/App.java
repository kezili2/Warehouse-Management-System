package com.warehouse.client;

import com.warehouse.client.ui.LoginFrame;

import javax.swing.*;

/** 客户端启动入口。 */
public class App {
    public static void main(String[] args) {
        // Swing 必须使用事件分发线程
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new LoginFrame().setVisible(true);
        });
    }
}
