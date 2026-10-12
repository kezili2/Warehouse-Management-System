package com.warehouse.client.ui;

import com.warehouse.client.net.ApiClient;
import com.warehouse.client.net.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/** 子系统3：多物料进出仓（一个单号多种物料同进/同出）。 */
public class MultiInoutPanel extends JPanel {
    private final ApiClient api = new ApiClient();

    private final JTextField billF = new JTextField(15);
    private final JTextField dateF = new JTextField(LocalDate.now().toString(), 10);
    private final JRadioButton inR  = new JRadioButton("进仓", true);
    private final JRadioButton outR = new JRadioButton("出仓");
    private final JTextField remarkF = new JTextField(20);
    private final JComboBox<String> handF = new JComboBox<>();
    private final JLabel curOp = new JLabel();

    /** 明细表：物料(代码-名称) | 数量 */
    private final DefaultTableModel items = new DefaultTableModel(
            new Object[]{"物料（代码 - 名称）", "数量"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return true; }
    };
    private final JTable itemsTable = new JTable(items);

    public MultiInoutPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 顶部：单头
        JPanel head = new JPanel(new GridLayout(3, 4, 6, 6));
        head.setBorder(BorderFactory.createTitledBorder("单头信息"));
        ButtonGroup bg = new ButtonGroup(); bg.add(inR); bg.add(outR);
        JButton genBtn = new JButton("生成单号");
        JButton addRow = new JButton("添加明细");
        JButton delRow = new JButton("删除明细");
        JButton okBtn  = new JButton("提交");

        head.add(new JLabel("单据类型")); head.add(panelOf(inR, outR));
        head.add(new JLabel("单    号")); head.add(panelOf(billF, genBtn));
        head.add(new JLabel("日    期")); head.add(dateF);
        head.add(new JLabel("操 作 员")); head.add(curOp);
        head.add(new JLabel("经 手 人")); head.add(handF);
        head.add(new JLabel("备    注")); head.add(remarkF);
        add(head, BorderLayout.NORTH);

        // 中部：明细表
        JPanel mid = new JPanel(new BorderLayout(4, 4));
        mid.setBorder(BorderFactory.createTitledBorder("物料明细（同一单号只能进仓或出仓，不可混用）"));
        JPanel tb = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tb.add(addRow); tb.add(delRow); tb.add(okBtn);
        mid.add(tb, BorderLayout.NORTH);
        itemsTable.setRowHeight(24);
        // 物料列用 JComboBox
        JComboBox<String> matEditor = new JComboBox<>();
        try {
            for (var m : api.searchMaterials(null)) matEditor.addItem(m.get("matCode") + " - " + m.get("matName"));
        } catch (Exception ignored) {}
        itemsTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(matEditor));
        mid.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        add(mid, BorderLayout.CENTER);

        // 初始化
        curOp.setText(Session.get().getRealName() + "（" + Session.get().getUserCode() + "）");
        loadHandlers();
        addBlankRow();

        // 事件
        genBtn.addActionListener(e -> genBill());
        inR.addActionListener(e -> genBill());
        outR.addActionListener(e -> genBill());
        addRow.addActionListener(e -> addBlankRow());
        delRow.addActionListener(e -> {
            int r = itemsTable.getSelectedRow();
            if (r >= 0) items.removeRow(r);
        });
        okBtn.addActionListener(e -> submit());
    }

    private void genBill() {
        try {
            String type = inR.isSelected() ? "MI" : "MO";
            billF.setText(api.nextBillNo(type, dateF.getText().trim()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "生成单号失败：" + ex.getMessage());
        }
    }

    private void loadHandlers() {
        try {
            handF.removeAllItems();
            for (var u : api.listUsers()) handF.addItem(u.get("userCode") + " - " + u.get("realName"));
        } catch (Exception ignored) {}
    }

    private void addBlankRow() {
        items.addRow(new Object[]{"", "1"});
    }

    @SuppressWarnings("unchecked")
    private void submit() {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < items.getRowCount(); i++) {
                Object matCell = items.getValueAt(i, 0);
                Object qtyCell = items.getValueAt(i, 1);
                if (matCell == null || matCell.toString().isEmpty()) continue;
                Map<String, Object> m = new HashMap<>();
                String mat = matCell.toString();
                m.put("matCode", mat.contains(" - ") ? mat.split(" - ")[0].trim() : mat.trim());
                m.put("qty",     Integer.parseInt(qtyCell.toString().trim()));
                list.add(m);
            }
            if (list.isEmpty()) { JOptionPane.showMessageDialog(this, "请至少添加一条物料明细"); return; }

            Map<String, Object> p = new HashMap<>();
            p.put("ioType",   inR.isSelected() ? "I" : "O");
            p.put("billNo",   billF.getText().trim());
            p.put("opDate",   dateF.getText().trim());
            p.put("operator", Session.get().getUserCode());
            p.put("handler",  String.valueOf(handF.getSelectedItem()).split(" - ")[0].trim());
            p.put("remark",   remarkF.getText().trim());
            p.put("items",    list);

            Map<String, Object> r = api.inoutMulti(p);
            JOptionPane.showMessageDialog(this, r.get("msg"));
            if ("OK".equals(r.get("flag"))) {
                items.setRowCount(0);
                remarkF.setText("");
                addBlankRow();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "明细中存在非法数量");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "提交失败：" + ex.getMessage());
        }
    }

    private static JPanel panelOf(java.awt.Component... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for (var c : cs) p.add(c);
        return p;
    }
}
