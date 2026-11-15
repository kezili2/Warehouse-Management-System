package com.warehouse.client.ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;

/** 日期选择控件：只读输入框 + "选"按钮，点击弹出月历点选日期。 */
public class DatePickerField extends JPanel {
    private LocalDate date;
    private final JTextField text = new JTextField(10);

    public DatePickerField(LocalDate init) {
        super(new FlowLayout(FlowLayout.LEFT, 2, 0));
        setOpaque(false);
        date = init;
        if (date != null) text.setText(date.toString());
        text.setEditable(false);
        JButton pick = new JButton("选");
        pick.setMargin(new Insets(1, 6, 1, 6));
        pick.addActionListener(e -> showCalendar());
        add(text);
        add(pick);
    }

    /** 兼容原 JTextField 的 getText 用法，返回 yyyy-MM-dd 或空串。 */
    public String getText() {
        return date == null ? "" : date.toString();
    }

    private void showCalendar() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        LocalDate picked = new CalDialog(owner, date == null ? LocalDate.now() : date).pick();
        if (picked != null) {
            date = picked;
            text.setText(date.toString());
        }
    }

    /** 月历弹窗：可前后翻月，也可直接选年、选月，点某天即选中并关闭。 */
    private static class CalDialog extends JDialog {
        private LocalDate result;
        private YearMonth shown;
        private final JPanel days = new JPanel(new GridLayout(7, 7, 2, 2));
        private final JComboBox<Integer> yearBox = new JComboBox<>();
        private final JComboBox<Integer> monthBox = new JComboBox<>();
        private boolean syncing = false;

        CalDialog(Window owner, LocalDate init) {
            super(owner, "选择日期", ModalityType.APPLICATION_MODAL);
            shown = YearMonth.from(init);
            setLayout(new BorderLayout(6, 6));
            ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JPanel nav = new JPanel(new BorderLayout(6, 6));
            JButton prev = new JButton("上月");
            JButton next = new JButton("下月");
            prev.setMargin(new Insets(1, 8, 1, 8));
            next.setMargin(new Insets(1, 8, 1, 8));
            prev.addActionListener(e -> { shown = shown.minusMonths(1); syncSelectors(); refresh(); });
            next.addActionListener(e -> { shown = shown.plusMonths(1); syncSelectors(); refresh(); });
            nav.add(prev, BorderLayout.WEST);
            nav.add(buildSelectors(), BorderLayout.CENTER);
            nav.add(next, BorderLayout.EAST);
            add(nav, BorderLayout.NORTH);
            add(days, BorderLayout.CENTER);

            syncSelectors();
            refresh();
            pack();
            setLocationRelativeTo(owner);
            setVisible(true);
        }

        /** 顶部中间的"年份 + 月份"下拉选择器。 */
        private JPanel buildSelectors() {
            int nowYear = LocalDate.now().getYear();
            for (int y = nowYear - 20; y <= nowYear + 10; y++) yearBox.addItem(y);
            for (int m = 1; m <= 12; m++) monthBox.addItem(m);
            yearBox.addActionListener(e -> {
                if (syncing) return;
                Integer y = (Integer) yearBox.getSelectedItem();
                if (y != null) { shown = shown.withYear(y); refresh(); }
            });
            monthBox.addActionListener(e -> {
                if (syncing) return;
                Integer m = (Integer) monthBox.getSelectedItem();
                if (m != null) { shown = shown.withMonth(m); refresh(); }
            });
            JPanel sel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            sel.add(yearBox);
            sel.add(new JLabel("年"));
            sel.add(monthBox);
            sel.add(new JLabel("月"));
            return sel;
        }

        /** 翻月按钮联动后，让下拉框显示 shown 对应的年月。 */
        private void syncSelectors() {
            syncing = true;
            yearBox.setSelectedItem(shown.getYear());
            monthBox.setSelectedItem(shown.getMonthValue());
            syncing = false;
        }

        private void refresh() {
            days.removeAll();
            for (String w : new String[]{"日", "一", "二", "三", "四", "五", "六"}) {
                JLabel h = new JLabel(w, SwingConstants.CENTER);
                h.setForeground(new Color(120, 120, 120));
                days.add(h);
            }
            // 该月 1 号前面补空格（周日在第一列）
            int lead = shown.atDay(1).getDayOfWeek().getValue() % 7;
            for (int i = 0; i < lead; i++) days.add(new JLabel(""));
            for (int d = 1; d <= shown.lengthOfMonth(); d++) {
                final LocalDate cur = shown.atDay(d);
                JButton b = new JButton(String.valueOf(d));
                b.setMargin(new Insets(1, 4, 1, 4));
                if (cur.equals(LocalDate.now())) b.setFont(b.getFont().deriveFont(Font.BOLD));
                b.addActionListener(e -> { result = cur; dispose(); });
                days.add(b);
            }
            days.revalidate();
            days.repaint();
        }

        LocalDate pick() {
            return result;
        }
    }
}
