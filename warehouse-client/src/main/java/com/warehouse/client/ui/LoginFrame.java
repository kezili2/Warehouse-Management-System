package com.warehouse.client.ui;

import com.warehouse.client.net.ApiClient;
import com.warehouse.client.net.Session;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/** 登录窗口。 */
public class LoginFrame extends JFrame {
    private final JTextField     userF   = new JTextField("admin");
    private final JPasswordField passF   = new JPasswordField("admin");
    private final JTextField     serverF = new JTextField("http://localhost:8080/warehouse");
    private final JLabel         status  = new JLabel(" ");

    public LoginFrame() {
        setTitle("仓库管理系统 — 登录");
        setSize(360, 230);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        form.add(new JLabel("服务器地址："));
        form.add(serverF);
        form.add(new JLabel("用户名："));
        form.add(userF);
        form.add(new JLabel("密  码："));
        form.add(passF);
        form.add(new JLabel(""));
        JButton loginBtn = new JButton("登 录");
        form.add(loginBtn);
        add(form, BorderLayout.CENTER);

        status.setForeground(new Color(0xA3, 0x2D, 0x2D));
        status.setHorizontalAlignment(SwingConstants.CENTER);
        add(status, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(loginBtn);
    }

    private void doLogin() {
        status.setText("登录中...");
        Session.get().setServerUrl(serverF.getText().trim());
        try {
            Map<String, Object> r = new ApiClient().login(userF.getText().trim(),
                                                          new String(passF.getPassword()));
            if (!"OK".equals(r.get("flag"))) {
                status.setText("登录失败：" + r.get("msg"));
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) r.get("data");
            Session.get().setUser((String) user.get("userCode"),
                                  (String) user.get("realName"),
                                  (String) user.get("address"));
            dispose();
            new MainFrame().setVisible(true);
        } catch (Exception ex) {
            status.setText("登录异常：" + ex.getMessage());
        }
    }
}
