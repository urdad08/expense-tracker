package expensetracker.model;

import java.time.LocalDate;

public record Expense(int id, int userId, int categoryId, String categoryName,
                      double amount, String description, LocalDate date) { }
