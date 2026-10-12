package com.warehouse.client.ui;

import javax.swing.*;
import java.awt.*;

/** 关于窗口：显示作者姓名 + 家庭住址（题目评分点要求）。 */
public class AboutDialog extends JDialog {
    public AboutDialog(Frame owner) {
        super(owner, "关于", true);
        setSize(360, 220);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(5, 1, 8, 8));
        JLabel title = new JLabel("仓库管理系统（客户机/服务器版）", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title);
        add(new JLabel(" 作 者：" + com.warehouse.client.net.Session.get().getRealName(), SwingConstants.CENTER));
        add(new JLabel(" 住 址：" + com.warehouse.client.net.Session.get().getAddress(), SwingConstants.CENTER));
        add(new JLabel(" 课 程：数据库系统 / 课程设计", SwingConstants.CENTER));
        JButton ok = new JButton("确定");
        ok.addActionListener(e -> dispose());
        add(ok);
    }
}
