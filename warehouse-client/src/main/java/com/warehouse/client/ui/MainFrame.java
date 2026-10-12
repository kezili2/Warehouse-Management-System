package com.warehouse.client.ui;

import com.warehouse.client.net.Session;

import javax.swing.*;
import java.awt.*;

/** 主窗口：菜单 + 3 个子系统 Tab + 底部状态栏。 */
public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("仓库管理系统 — " + Session.get().getRealName());
        setSize(960, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ---- 菜单 ----
        JMenuBar mb = new JMenuBar();
        JMenu sys = new JMenu("系统");
        JMenuItem about = new JMenuItem("关于");
        JMenuItem logout = new JMenuItem("退出登录");
        JMenuItem exit   = new JMenuItem("退出系统");
        about.addActionListener(e -> new AboutDialog(this).setVisible(true));
        logout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        exit.addActionListener(e -> System.exit(0));
        sys.add(about); sys.addSeparator(); sys.add(logout); sys.add(exit);
        mb.add(sys);
        setJMenuBar(mb);

        // ---- 3 个 Tab ----
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("物料档案管理",   new MaterialPanel());
        tabs.addTab("单物料进出仓",   new SingleInoutPanel());
        tabs.addTab("多物料进出仓",   new MultiInoutPanel());
        add(tabs, BorderLayout.CENTER);

        // ---- 状态栏 ----
        JLabel sb = new JLabel(" 当前操作人员：" + Session.get().getRealName()
                + "（" + Session.get().getUserCode() + "）   服务器：" + Session.get().getServerUrl());
        sb.setBorder(BorderFactory.createEtchedBorder());
        add(sb, BorderLayout.SOUTH);
    }
}
