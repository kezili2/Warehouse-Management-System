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
    private final JTextField qtyF           = new JTextField(20);
    private final JComboBox<String> handF   = new JComboBox<>();
    private final JTextField remarkF        = new JTextField(20);
    private final JLabel   curOp            = new JLabel();
    /** 查询栏专用的物料筛选下拉，与表单区的 matF 分开，避免组件被重复添加导致布局错乱。 */
    private final JComboBox<String> matFilterQ = new JComboBox<>();

    private final DefaultTableModel recModel = new DefaultTableModel(
            new Object[]{"单号", "类型", "物料", "数量", "日期", "操作人员", "经手人", "备注"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public SingleInoutPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 顶部：单据录入（GridBagLayout，标签右对齐，两列字段等宽对齐）
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("单据录入"));
        ButtonGroup bg = new ButtonGroup(); bg.add(inR); bg.add(outR);
        JButton genBtn = new JButton("生成单号");
        JButton okBtn  = new JButton("提交");

        addRow(form, 0, "单据类型", panelOf(inR, outR), "单    号", panelOf(billF, genBtn));
        addRow(form, 1, "日    期", dateF,             "操 作 员", curOp);
        addRow(form, 2, "物    料", matF,              "数    量", panelOf(qtyF));
        addRow(form, 3, "经 手 人", handF,             "备    注", panelOf(remarkF, okBtn));

        add(form, BorderLayout.NORTH);

        // 中部：记录查询 + 表格
        JPanel mid = new JPanel(new BorderLayout(4, 4));
        mid.setBorder(BorderFactory.createTitledBorder("进出仓记录"));
        JPanel q = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JTextField fromF = new JTextField(12), toF = new JTextField(12), rkF = new JTextField(9);
        fromF.setText(LocalDate.now().withDayOfMonth(1).toString());
        toF.setText(LocalDate.now().toString());
        JButton qBtn = new JButton("查询");
        q.add(new JLabel("起"));  q.add(fromF);
        q.add(new JLabel("止"));  q.add(toF);
        q.add(new JLabel("物料")); q.add(matFilterQ);
        q.add(new JLabel("备注")); q.add(rkF);
        q.add(qBtn);
        mid.add(q, BorderLayout.NORTH);
        mid.add(new JScrollPane(new JTable(recModel)), BorderLayout.CENTER);
        add(mid, BorderLayout.CENTER);

        // 初始化
        curOp.setText(Session.get().getRealName() + "（" + Session.get().getUserCode() + "）");
        matF.setEditable(true);
        matFilterQ.setEditable(true);
        loadMaterials();
        loadHandlers();

        // 事件
        genBtn.addActionListener(e -> genBill());
        okBtn.addActionListener(e -> submit());
        inR.addActionListener(e -> genBill());
        outR.addActionListener(e -> genBill());
        qBtn.addActionListener(e -> reloadRecords(fromF.getText(), toF.getText(),
                String.valueOf(matFilterQ.getSelectedItem()), rkF.getText()));
    }

    /** 向 GridBagLayout 表单添加一行：左标签、左字段、右标签、右字段，标签右对齐。 */
    private void addRow(JPanel form, int row, String l1, JComponent c1, String l2, JComponent c2) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 4);
        g.anchor = GridBagConstraints.EAST;
        g.gridx = 0; g.gridy = row; form.add(new JLabel(l1), g);
        g.anchor = GridBagConstraints.WEST;
        g.fill  = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.gridx = 1; form.add(c1, g);
        g.anchor = GridBagConstraints.EAST;
        g.fill  = GridBagConstraints.NONE;
        g.weightx = 0;
        g.gridx = 2; form.add(new JLabel(l2), g);
        g.anchor = GridBagConstraints.WEST;
        g.fill  = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.gridx = 3; form.add(c2, g);
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
            matFilterQ.removeAllItems();
            matFilterQ.addItem("");
            for (var m : api.searchMaterials(null)) {
                String item = m.get("matCode") + " - " + m.get("matName");
                matF.addItem(item);
                matFilterQ.addItem(item);
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
