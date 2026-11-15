package com.warehouse.client.ui;

import com.warehouse.client.net.ApiClient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;

/** 进出仓记录查询：独立 Tab，与三大功能并列。 */
public class RecordPanel extends JPanel {
    private final ApiClient api = new ApiClient();

    private final DatePickerField fromF = new DatePickerField(LocalDate.now().withDayOfMonth(1));
    private final DatePickerField toF   = new DatePickerField(LocalDate.now());
    private final JComboBox<String> matF = new JComboBox<>();
    private final JTextField rkF   = new JTextField(12);

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"单号", "类型", "物料", "数量", "日期", "操作人员", "经手人", "备注"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public RecordPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 顶部：查询条件
        JPanel q = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        q.setBorder(BorderFactory.createTitledBorder("查询条件"));
        JButton qBtn = new JButton("查询");
        q.add(new JLabel("起"));  q.add(fromF);
        q.add(new JLabel("止"));  q.add(toF);
        q.add(new JLabel("物料")); q.add(matF);
        q.add(new JLabel("备注")); q.add(rkF);
        q.add(qBtn);
        add(q, BorderLayout.NORTH);

        // 中部：结果表格
        JScrollPane sp = new JScrollPane(new JTable(model));
        sp.setBorder(BorderFactory.createTitledBorder("查询结果"));
        add(sp, BorderLayout.CENTER);

        // 初始化
        matF.setEditable(true);
        loadMaterials();

        // 事件
        qBtn.addActionListener(e -> reload());
        // 面板首次显示时自动查一遍当月记录
        reload();
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

    private void reload() {
        try {
            String mat = String.valueOf(matF.getSelectedItem());
            if (mat != null && mat.contains(" - ")) mat = mat.split(" - ")[0];
            Map<String, Object> p = new HashMap<>();
            p.put("dateFrom", fromF.getText().trim());
            p.put("dateTo",   toF.getText().trim());
            p.put("matCode",  mat == null ? "" : mat.trim());
            p.put("remark",   rkF.getText().trim());
            model.setRowCount(0);
            for (var r : api.searchRecords(p)) {
                model.addRow(new Object[]{r.get("billNo"), "I".equals(r.get("ioType")) ? "进" : "出",
                        r.get("matCode"), r.get("qty"), r.get("opDate"),
                        r.get("operator"), r.get("handler"), r.get("remark")});
            }
            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "该条件下没有记录");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "查询失败：" + ex.getMessage());
        }
    }
}
