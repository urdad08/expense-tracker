package expensetracker.ui;

import expensetracker.dao.CategoryDAO;
import expensetracker.model.Category;
import expensetracker.model.User;
import expensetracker.util.Validator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class CategoryPanel extends JPanel {
    private final User user;
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Category", "Monthly limit"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<Category> categories = new ArrayList<>();

    public CategoryPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton add = new JButton("Add category");
        JButton limit = new JButton("Set limit");
        JButton del = new JButton("Delete");
        add.addActionListener(e -> onAdd());
        limit.addActionListener(e -> onSetLimit());
        del.addActionListener(e -> onDelete());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(add);
        buttons.add(limit);
        buttons.add(del);
        add(buttons, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        try {
            categories = CategoryDAO.list(user.id());
            model.setRowCount(0);
            for (Category c : categories) {
                model.addRow(new Object[]{c.name(), c.monthlyLimit() > 0 ? String.format("%.2f", c.monthlyLimit()) : "none"});
            }
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void onAdd() {
        JTextField name = new JTextField();
        JTextField limit = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 8));
        form.add(new JLabel("Name"));
        form.add(name);
        form.add(new JLabel("Monthly limit (optional)"));
        form.add(limit);
        if (JOptionPane.showConfirmDialog(this, form, "Add Category",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            CategoryDAO.add(user.id(), Validator.categoryName(name.getText()), Validator.parseLimit(limit.getText()));
            refresh();
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void onSetLimit() {
        Category c = selected();
        if (c == null) return;
        String input = JOptionPane.showInputDialog(this, "Monthly limit for " + c.name() + " (blank = none):",
                c.monthlyLimit() > 0 ? String.format("%.2f", c.monthlyLimit()) : "");
        if (input == null) return;
        try {
            CategoryDAO.setLimit(c.id(), user.id(), Validator.parseLimit(input));
            refresh();
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void onDelete() {
        Category c = selected();
        if (c == null) return;
        if (JOptionPane.showConfirmDialog(this, "Delete category '" + c.name() + "'?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            CategoryDAO.delete(c.id(), user.id());
            refresh();
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private Category selected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= categories.size()) {
            Dialogs.error(this, "Select a category first.");
            return null;
        }
        return categories.get(row);
    }
}
