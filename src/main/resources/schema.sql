-- Expense Tracker schema (SQLite)
CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name          TEXT NOT NULL,
    monthly_limit REAL NOT NULL DEFAULT 0 CHECK (monthly_limit >= 0),
    UNIQUE (user_id, name)
);

CREATE TABLE IF NOT EXISTS expenses (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id  INTEGER NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    amount       REAL NOT NULL CHECK (amount > 0),
    description  TEXT NOT NULL DEFAULT '',
    expense_date TEXT NOT NULL,
    created_at   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_expenses_user_date ON expenses(user_id, expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category_id);
