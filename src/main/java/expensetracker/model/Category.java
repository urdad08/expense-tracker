package expensetracker.model;

public record Category(int id, int userId, String name, double monthlyLimit) {
    @Override
    public String toString() { return name; }
}
