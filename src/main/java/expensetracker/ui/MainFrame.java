package expensetracker.ui;

import expensetracker.model.User;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;

public class MainFrame extends JFrame {
    public MainFrame(User user) {
        super("Expense Tracker - " + user.username());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        ExpensePanel expenses = new ExpensePanel(user);
        SummaryPanel summary = new SummaryPanel(user);
        CategoryPanel categories = new CategoryPanel(user);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Expenses", expenses);
        tabs.addTab("Summary", summary);
        tabs.addTab("Categories", categories);
        // refresh whichever tab the user switches to so data is never stale
        tabs.addChangeListener(e -> {
            switch (tabs.getSelectedIndex()) {
                case 0 -> expenses.refresh();
                case 1 -> summary.refresh();
                default -> categories.refresh();
            }
        });
        setContentPane(tabs);

        JMenuBar bar = new JMenuBar();
        JMenu account = new JMenu("Account");
        JMenuItem logout = new JMenuItem("Log out");
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        account.add(logout);
        bar.add(account);
        setJMenuBar(bar);

        setSize(900, 560);
        setLocationRelativeTo(null);
    }
}
