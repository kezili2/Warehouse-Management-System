package com.warehouse.client.ui;

import com.warehouse.client.net.ApiClient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/** 子系统1：物料档案管理（增删改查 + 模糊查询）。 */
public class MaterialPanel extends JPanel {
    private final ApiClient api = new ApiClient();

    private final JTextField  qF       = new JTextField(20);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"代码", "名称", "规格", "单位", "库存", "备注"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    private final JTextField codeF = new JTextField(8);
    private final JTextField nameF = new JTextField(12);
    private final JTextField specF = new JTextField(10);
    private final JComboBox<String> unitF = new JComboBox<>(
            new String[]{"件", "套", "公斤", "吨", "升", "米", "毫米", "个"});
    private final JTextField stockF = new JTextField(6);
    private final JTextField remarkF = new JTextField(20);

    public MaterialPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 顶部：搜索
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("模糊查询："));
        top.add(qF);
        JButton searchBtn = new JButton("查询");
        JButton addBtn    = new JButton("新增");
        JButton updateBtn = new JButton("修改");
        JButton deleteBtn = new JButton("删除");
        JButton clearBtn  = new JButton("清空表单");
        top.add(searchBtn); top.add(addBtn); top.add(updateBtn); top.add(deleteBtn); top.add(clearBtn);
        add(top, BorderLayout.NORTH);

        // 中部：表格
        table.setRowHeight(22);
        table.getSelectionModel().addListSelectionListener(e -> fillFormFromTable());
        add(new JScrollPane(table), BorderLayout.CENTER);

        // 底部：表单
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.setBorder(BorderFactory.createTitledBorder("物料信息"));
        form.add(new JLabel("代码"));   form.add(codeF);
        form.add(new JLabel("名称"));   form.add(nameF);
        form.add(new JLabel("规格"));   form.add(specF);
        form.add(new JLabel("单位"));   form.add(unitF);
        form.add(new JLabel("库存"));   form.add(stockF);
        form.add(new JLabel("备注"));   form.add(remarkF);
        add(form, BorderLayout.SOUTH);

        unitF.setEditable(true);  // 题目要求：计量单位下拉 + 可手动输入

        // 事件
        searchBtn.addActionListener(e -> reload());
        addBtn.addActionListener(e -> doAdd());
        updateBtn.addActionListener(e -> doUpdate());
        deleteBtn.addActionListener(e -> doDelete());
        clearBtn.addActionListener(e -> clearForm());

        reload();
    }

    private void reload() {
        try {
            List<Map<String, Object>> data = api.searchMaterials(qF.getText().trim());
            model.setRowCount(0);
            for (var m : data) {
                model.addRow(new Object[]{
                        m.get("matCode"), m.get("matName"), m.get("spec"),
                        m.get("unit"), m.get("stock"), m.get("remark")});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载失败：" + ex.getMessage());
        }
    }

    private void fillFormFromTable() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        codeF.setText(String.valueOf(model.getValueAt(r, 0)));
        nameF.setText(String.valueOf(model.getValueAt(r, 1)));
        specF.setText(String.valueOf(model.getValueAt(r, 2)));
        unitF.setSelectedItem(model.getValueAt(r, 3));
        stockF.setText(String.valueOf(model.getValueAt(r, 4)));
        remarkF.setText(String.valueOf(model.getValueAt(r, 5)));
        codeF.setEditable(false);  // 修改时不能改代码
    }

    private void clearForm() {
        codeF.setText(""); nameF.setText(""); specF.setText("");
        unitF.setSelectedItem("个"); stockF.setText("0"); remarkF.setText("");
        codeF.setEditable(true);
        table.clearSelection();
    }

    private void doAdd() {
        try {
            Map<String, Object> m = new HashMap<>();
            if (!codeF.getText().trim().isEmpty()) m.put("matCode", codeF.getText().trim());
            m.put("matName", nameF.getText().trim());
            m.put("spec",    specF.getText().trim());
            m.put("unit",    unitF.getSelectedItem().toString().trim());
            m.put("stock",   Integer.parseInt(stockF.getText().trim()));
            m.put("remark",  remarkF.getText().trim());
            Map<String, Object> r = api.saveMaterial(m);
            JOptionPane.showMessageDialog(this, r.get("msg"));
            if ("OK".equals(r.get("flag"))) { clearForm(); reload(); }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "库存必须是整数");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "新增失败：" + ex.getMessage());
        }
    }

    private void doUpdate() {
        String code = codeF.getText().trim();
        if (code.isEmpty()) { JOptionPane.showMessageDialog(this, "请先选中一条记录"); return; }
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("matName", nameF.getText().trim());
            m.put("spec",    specF.getText().trim());
            m.put("unit",    unitF.getSelectedItem().toString().trim());
            m.put("stock",   Integer.parseInt(stockF.getText().trim()));
            m.put("remark",  remarkF.getText().trim());
            Map<String, Object> r = api.updateMaterial(code, m);
            JOptionPane.showMessageDialog(this, r.get("msg"));
            if ("OK".equals(r.get("flag"))) { clearForm(); reload(); }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "修改失败：" + ex.getMessage());
        }
    }

    private void doDelete() {
        String code = codeF.getText().trim();
        if (code.isEmpty()) { JOptionPane.showMessageDialog(this, "请先选中一条记录"); return; }
        if (JOptionPane.showConfirmDialog(this, "确定删除 " + code + " 吗？",
                "确认", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            Map<String, Object> r = api.deleteMaterial(code);
            JOptionPane.showMessageDialog(this, r.get("msg"));
            if ("OK".equals(r.get("flag"))) { clearForm(); reload(); }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "删除失败：" + ex.getMessage());
        }
    }
}
