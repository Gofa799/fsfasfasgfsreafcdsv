package bot;

public class WithdrawalRequest {
    private long userId;
    private int amount;
    private String date;

    public WithdrawalRequest(long userId, int amount, String date) {
        this.userId = userId;
        this.amount = amount;
        this.date = date;
    }

    public long getUserId() {
        return userId;
    }

    public int getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }
}
