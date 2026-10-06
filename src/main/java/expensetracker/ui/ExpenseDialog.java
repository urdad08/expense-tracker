package expensetracker.ui;

import expensetracker.model.Category;
import expensetracker.model.Expense;
import expensetracker.util.Validator;

import java.awt.Component;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Add / edit form. Returns the validated Expense, or null if the user cancelled. */
final class ExpenseDialog {
    private ExpenseDialog() { }

    static Expense show(Component parent, int userId, List<Category> categories, Expense existing) {
        JTextField date = new JTextField(existing == null ? LocalDate.now().toString() : existing.date().toString(), 12);
        JComboBox<Category> cat = new JComboBox<>(categories.toArray(new Category[0]));
        JTextField amount = new JTextField(existing == null ? "" : String.format("%.2f", existing.amount()));
        JTextField desc = new JTextField(existing == null ? "" : existing.description());

        if (existing != null) {
            for (int i = 0; i < cat.getItemCount(); i++) {
                if (cat.getItemAt(i).id() == existing.categoryId()) cat.setSelectedIndex(i);
            }
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 8));
        form.add(new JLabel("Date (yyyy-MM-dd)")); form.add(date);
        form.add(new JLabel("Category"));          form.add(cat);
        form.add(new JLabel("Amount"));            form.add(amount);
        form.add(new JLabel("Description"));       form.add(desc);

        while (true) {
            int result = JOptionPane.showConfirmDialog(parent, form,
                    existing == null ? "Add Expense" : "Edit Expense",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return null;
            try {
                LocalDate d = Validator.parseDate(date.getText(), "Date");
                double a = Validator.parseAmount(amount.getText());
                String text = Validator.description(desc.getText());
                Category c = (Category) cat.getSelectedItem();
                return new Expense(existing == null ? 0 : existing.id(), userId,
                        c.id(), c.name(), a, text, d);
            } catch (IllegalArgumentException ex) {
                Dialogs.error(parent, ex.getMessage());   // validation failed: show message, re-open the form
            }
        }
    }
}
