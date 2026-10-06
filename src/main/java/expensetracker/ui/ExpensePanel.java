package expensetracker.ui;

import expensetracker.dao.CategoryDAO;
import expensetracker.dao.ExpenseDAO;
import expensetracker.model.Category;
import expensetracker.model.Expense;
import expensetracker.model.User;
import expensetracker.util.Validator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class ExpensePanel extends JPanel {
    private final User user;
    private final JTextField fromField = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 9);
    private final JTextField toField = new JTextField(LocalDate.now().toString(), 9);
    private final JComboBox<Category> categoryBox = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Date", "Category", "Description", "Amount"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel totalLabel = new JLabel(" ");
    private List<Expense> shown = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();

    public ExpensePanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("From"));
        filters.add(fromField);
        filters.add(new JLabel("To"));
        filters.add(toField);
        filters.add(new JLabel("Category"));
        filters.add(categoryBox);
        JButton apply = new JButton("Apply filter");
        apply.addActionListener(e -> reload());
        filters.add(apply);
        add(filters, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton delBtn = new JButton("Delete");
        addBtn.addActionListener(e -> onAdd());
        editBtn.addActionListener(e -> onEdit());
        delBtn.addActionListener(e -> onDelete());
        JPanel south = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(delBtn);
        south.add(buttons, BorderLayout.WEST);
        south.add(totalLabel, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        refresh();
    }

    /** Reload categories (they may have changed on another tab) and the table. */
    public void refresh() {
        try {
            Category selected = (Category) categoryBox.getSelectedItem();
            categories = CategoryDAO.list(user.id());
            categoryBox.removeAllItems();
            categoryBox.addItem(new Category(0, user.id(), "All categories", 0));
            categories.forEach(categoryBox::addItem);
            if (selected != null) {
                for (int i = 0; i < categoryBox.getItemCount(); i++) {
                    if (categoryBox.getItemAt(i).id() == selected.id()) categoryBox.setSelectedIndex(i);
                }
            }
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
        reload();
    }

    private void reload() {
        try {
            LocalDate from = Validator.parseDate(fromField.getText(), "From date");
            LocalDate to = Validator.parseDate(toField.getText(), "To date");
            if (from.isAfter(to)) throw new IllegalArgumentException("'From' date must not be after 'To' date.");
            Category c = (Category) categoryBox.getSelectedItem();
            Integer categoryId = (c == null || c.id() == 0) ? null : c.id();

            shown = ExpenseDAO.search(user.id(), from, to, categoryId);
            model.setRowCount(0);
            double total = 0;
            for (Expense e : shown) {
                model.addRow(new Object[]{e.date(), e.categoryName(), e.description(), String.format("%.2f", e.amount())});
                total += e.amount();
            }
            totalLabel.setText(String.format("Total: %.2f   (%d expenses)", total, shown.size()));
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void onAdd() {
        if (categories.isEmpty()) {
            Dialogs.error(this, "Create a category first (Categories tab).");
            return;
        }
        Expense e = ExpenseDialog.show(this, user.id(), categories, null);
        if (e == null) return;
        try {
            ExpenseDAO.add(e);
            reload();
            checkBudget(e);
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void onEdit() {
        Expense selected = selectedExpense();
        if (selected == null) return;
        Expense e = ExpenseDialog.show(this, user.id(), categories, selected);
        if (e == null) return;
        try {
            ExpenseDAO.update(e);
            reload();
            checkBudget(e);
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void onDelete() {
        Expense selected = selectedExpense();
        if (selected == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "Delete this expense?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            ExpenseDAO.delete(selected.id(), user.id());
            reload();
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private Expense selectedExpense() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) {
            Dialogs.error(this, "Select an expense in the table first.");
            return null;
        }
        return shown.get(row);
    }

    /** Spending-limit alert: warn if this category's monthly total now exceeds its limit. */
    private void checkBudget(Expense e) throws SQLException {
        Category cat = categories.stream().filter(c -> c.id() == e.categoryId()).findFirst().orElse(null);
        if (cat == null || cat.monthlyLimit() <= 0) return;
        double spent = ExpenseDAO.spentInMonth(user.id(), cat.id(), YearMonth.from(e.date()));
        if (spent > cat.monthlyLimit()) {
            Dialogs.warn(this, String.format("'%s' is over its monthly limit for %s:%nspent %.2f of %.2f.",
                    cat.name(), YearMonth.from(e.date()), spent, cat.monthlyLimit()));
        }
    }
}
