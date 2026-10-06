package expensetracker.ui;

import expensetracker.dao.UserDAO;
import expensetracker.model.User;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class LoginFrame extends JFrame {
    private final JTextField userField = new JTextField(16);
    private final JPasswordField passField = new JPasswordField(16);

    public LoginFrame() {
        super("Expense Tracker - Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Expense Tracker");
        title.setFont(title.getFont().deriveFont(20f));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        p.add(title, c);

        c.gridwidth = 1;
        c.gridy = 1; c.gridx = 0; p.add(new JLabel("Username"), c);
        c.gridx = 1; p.add(userField, c);
        c.gridy = 2; c.gridx = 0; p.add(new JLabel("Password"), c);
        c.gridx = 1; p.add(passField, c);

        JButton login = new JButton("Login");
        JButton register = new JButton("Register");
        JPanel buttons = new JPanel();
        buttons.add(login);
        buttons.add(register);
        c.gridy = 3; c.gridx = 0; c.gridwidth = 2;
        p.add(buttons, c);

        login.addActionListener(e -> doLogin());
        register.addActionListener(e -> doRegister());
        getRootPane().setDefaultButton(login);

        setContentPane(p);
        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        try {
            UserDAO.login(userField.getText(), new String(passField.getPassword())).ifPresentOrElse(
                    this::openMain,
                    () -> Dialogs.error(this, "Invalid username or password."));
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void doRegister() {
        try {
            User u = UserDAO.register(userField.getText(), new String(passField.getPassword()));
            openMain(u);
        } catch (IllegalArgumentException ex) {
            Dialogs.error(this, ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.error(this, "Database error: " + ex.getMessage());
        }
    }

    private void openMain(User user) {
        dispose();
        new MainFrame(user).setVisible(true);
    }
}
