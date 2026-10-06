package expensetracker.model;

public record CategorySummary(String name, double limit, double spent) {
    public boolean isOverLimit() { return limit > 0 && spent > limit; }
}
