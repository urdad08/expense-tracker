# Expense Tracker (Java Swing + SQLite + JDBC)

A desktop app for tracking daily spending against monthly category budgets.

## Requirements
- JDK 17 or newer
- Maven 3.8+ (downloads the SQLite JDBC driver automatically)

## Run
```
mvn compile exec:java -Dexec.mainClass=expensetracker.Main
```
Or build a single runnable jar:
```
mvn package
java -jar target/expense-tracker-1.0.jar
```
The database file `expense_tracker.db` is created in the folder you run from.

## Features
| Requirement from proposal | Where it lives |
|---|---|
| Users, Categories, Expenses linked by foreign keys | `src/main/resources/schema.sql` |
| Full CRUD | `ExpenseDAO`, `CategoryDAO`, `UserDAO` |
| Input validation | `util/Validator.java`, `ExpenseDialog` |
| Date-filtered reports | `ExpenseDAO.search`, filter bar in `ExpensePanel` |
| Category-wise breakdown | `ExpenseDAO.categoryBreakdown` (LEFT JOIN + GROUP BY) |
| Monthly totals | `ExpenseDAO.monthlyTotals` (GROUP BY substr(date,1,7)) |
| Spending limits | `categories.monthly_limit`, `ExpenseDAO.spentInMonth`, alert in `ExpensePanel.checkBudget` |

## Structure
```
expensetracker/
  Main.java
  model/  User, Category, Expense, CategorySummary   (records)
  db/     Database          (JDBC connection + schema init)
  dao/    UserDAO, CategoryDAO, ExpenseDAO   (all SQL, PreparedStatements only)
  util/   Validator
  ui/     LoginFrame, MainFrame, ExpensePanel, SummaryPanel, CategoryPanel, ExpenseDialog
```

## Design notes
- Passwords are salted and hashed with PBKDF2 (never stored in plain text).
- Every query uses PreparedStatement, so it is safe from SQL injection.
- Every query is scoped by `user_id`, so users only see their own data.
- Dates are stored as ISO `yyyy-MM-dd` text, so `BETWEEN` filtering sorts correctly.
- Categories with expenses cannot be deleted (`ON DELETE RESTRICT`).
