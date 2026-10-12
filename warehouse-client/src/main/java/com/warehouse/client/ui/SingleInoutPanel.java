package com.warehouse.client.ui;

import com.warehouse.client.net.ApiClient;
import com.warehouse.client.net.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/** 子系统2：单物料进出仓。 */
public class SingleInoutPanel extends JPanel {
    private final ApiClient api = new ApiClient();

    private final JTextField billF   = new JTextField(15);
    private final JTextField dateF   = new JTextField(LocalDate.now().toString(), 10);
    private final JRadioButton inR  = new JRadioButton("进仓", true);
    private final JRadioButton outR = new JRadioButton("出仓");
    private final JComboBox<String> matF   = new JComboBox<>();
    private final JTextField qtyF           = new JTextField(6);
    private final JComboBox<String> handF   = new JComboBox<>();
    private final JTextField remarkF        = new JTextField(20);
    private final JLabel   curOp            = new JLabel();

    private final DefaultTableModel recModel = new DefaultTableModel(
            new Object[]{"单号", "类型", "物料", "数量", "日期", "操作人员", "经手人", "备注"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public SingleInoutPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 顶部：单据录入
        JPanel form = new JPanel(new GridLayout(4, 4, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("单据录入"));
        ButtonGroup bg = new ButtonGroup(); bg.add(inR); bg.add(outR);
        JButton genBtn = new JButton("生成单号");
        JButton okBtn  = new JButton("提交");

        form.add(new JLabel("单据类型")); form.add(panelOf(inR, outR));
        form.add(new JLabel("单    号")); form.add(panelOf(billF, genBtn));
        form.add(new JLabel("日    期")); form.add(dateF);
        form.add(new JLabel("操 作 员")); form.add(curOp);

        form.add(new JLabel("物    料")); form.add(matF);
        form.add(new JLabel("数    量")); form.add(qtyF);
        form.add(new JLabel("经 手 人")); form.add(handF);
        form.add(new JLabel("备    注")); form.add(panelOf(remarkF, okBtn));

        add(form, BorderLayout.NORTH);

        // 中部：记录查询 + 表格
        JPanel mid = new JPanel(new BorderLayout(4, 4));
        mid.setBorder(BorderFactory.createTitledBorder("进出仓记录"));
        JPanel q = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField fromF = new JTextField(8), toF = new JTextField(8), rkF = new JTextField(8);
        fromF.setText(LocalDate.now().withDayOfMonth(1).toString());
        toF.setText(LocalDate.now().toString());
        JButton qBtn = new JButton("查询");
        q.add(new JLabel("起")); q.add(fromF);
        q.add(new JLabel("止")); q.add(toF);
        q.add(new JLabel("物料")); q.add(matF);
        q.add(new JLabel("备注")); q.add(rkF);
        q.add(qBtn);
        mid.add(q, BorderLayout.NORTH);
        mid.add(new JScrollPane(new JTable(recModel)), BorderLayout.CENTER);
        add(mid, BorderLayout.CENTER);

        // 初始化
        curOp.setText(Session.get().getRealName() + "（" + Session.get().getUserCode() + "）");
        matF.setEditable(true);
        loadMaterials();
        loadHandlers();

        // 事件
        genBtn.addActionListener(e -> genBill());
        okBtn.addActionListener(e -> submit());
        inR.addActionListener(e -> genBill());
        outR.addActionListener(e -> genBill());
        qBtn.addActionListener(e -> reloadRecords(fromF.getText(), toF.getText(),
                String.valueOf(matF.getSelectedItem()), rkF.getText()));
    }

    private void genBill() {
        try {
            String type = inR.isSelected() ? "I" : "O";
            billF.setText(api.nextBillNo(type, dateF.getText().trim()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "生成单号失败：" + ex.getMessage());
        }
    }

    private void loadMaterials() {
        try {
            matF.removeAllItems();
            matF.addItem("");
            for (var m : api.searchMaterials(null)) {
                matF.addItem(m.get("matCode") + " - " + m.get("matName"));
            }
        } catch (Exception ignored) {}
    }

    private void loadHandlers() {
        try {
            handF.removeAllItems();
            for (var u : api.listUsers()) handF.addItem(u.get("userCode") + " - " + u.get("realName"));
        } catch (Exception ignored) {}
    }

    private void submit() {
        try {
            String type = inR.isSelected() ? "I" : "O";
            Map<String, Object> p = new HashMap<>();
            p.put("ioType",   type);
            p.put("billNo",   billF.getText().trim());
            p.put("matCode",  String.valueOf(matF.getSelectedItem()).split(" - ")[0].trim());
            p.put("qty",      Integer.parseInt(qtyF.getText().trim()));
            p.put("opDate",   dateF.getText().trim());
            p.put("operator", Session.get().getUserCode());
            p.put("handler",  String.valueOf(handF.getSelectedItem()).split(" - ")[0].trim());
            p.put("remark",   remarkF.getText().trim());
            Map<String, Object> r = api.inoutSingle(p);
            JOptionPane.showMessageDialog(this, r.get("msg"));
            if ("OK".equals(r.get("flag"))) {
                remarkF.setText(""); qtyF.setText("");
                loadMaterials();   // 刷新库存
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "数量必须是整数");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "提交失败：" + ex.getMessage());
        }
    }

    private void reloadRecords(String from, String to, String mat, String remark) {
        try {
            Map<String, Object> q = new HashMap<>();
            q.put("dateFrom", from);
            q.put("dateTo",   to);
            if (mat != null && !mat.isEmpty() && mat.contains(" - ")) mat = mat.split(" - ")[0];
            q.put("matCode",  mat);
            q.put("remark",   remark);
            recModel.setRowCount(0);
            for (var r : api.searchRecords(q)) {
                recModel.addRow(new Object[]{r.get("billNo"), "I".equals(r.get("ioType")) ? "进" : "出",
                        r.get("matCode"), r.get("qty"), r.get("opDate"),
                        r.get("operator"), r.get("handler"), r.get("remark")});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "查询失败：" + ex.getMessage());
        }
    }

    private static JPanel panelOf(java.awt.Component... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for (var c : cs) p.add(c);
        return p;
    }
}
