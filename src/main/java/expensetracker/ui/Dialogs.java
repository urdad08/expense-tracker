package expensetracker.ui;

import java.awt.Component;
import javax.swing.JOptionPane;

final class Dialogs {
    private Dialogs() { }

    static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Budget warning", JOptionPane.WARNING_MESSAGE);
    }
}
