package expensetracker.ui;

import expensetracker.dao.ExpenseDAO;
import expensetracker.model.CategorySummary;
import expensetracker.model.User;
import expensetracker.util.Validator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class SummaryPanel extends JPanel {
    private final User user;
    private final JTextField monthField = new JTextField(YearMonth.now().toString(), 8);
    private final JLabel totalLabel = new JLabel(" ");
    private final DefaultTableModel breakdown = readOnlyModel("Category", "Monthly limit", "Spent", "Status");
    private final DefaultTableModel months = readOnlyModel("Month", "Total spent");

    public SummaryPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Month (yyyy-MM)"));
        top.add(monthField);
        JButton show = new JButton("Show");
        show.addActionListener(e -> refresh());
        top.add(show);
        top.add(totalLabel);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 8, 8));
        center.add(titled("Category-wise breakdown", new JScrollPane(new JTable(breakdown))));
        center.add(titled("Monthly totals (recent months)", new JScrollPane(new JTable(months))));
        add(center, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        try {
            YearMonth ym = Validator.parseMonth(monthField.getText());
            LocalDate from = ym.atDay(1);
            LocalDate to = ym.atEndOfMonth();

            List<CategorySummary> rows = ExpenseDAO.categoryBreakdown(user.id(), from, to);
            breakdown.setRowCount(0);
            double total = 0;
            for (CategorySummary s : rows) {
                String limit = s.limit() > 0 ? String.format("%.2f", s.limit()) : "-";
                String status = s.limit() <= 0 ? "-" : (s.isOverLimit() ? "OVER LIMIT" : "OK");
                breakdown.addRow(new Object[]{s.name(), limit, String.format("%.2f", s.spent()), status});
                total += s.spent();
            }
            totalLabel.setText(String.format("   Total for %s: %.2f", ym, total));

            months.setRowCount(0);
            for (Map.Entry<String, Double> e : ExpenseDAO.monthlyTotals(user.id(), 12).entrySet()) {
                months.addRow(new Object[]{e.getKey(), String.format("%.2f", e.getValue())});
            }
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private static DefaultTableModel readOnlyModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static JPanel titled(String title, JScrollPane content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(content);
        return p;
    }
}
