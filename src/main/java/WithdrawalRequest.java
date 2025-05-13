public class WithdrawalRequest {
    private long userId;
    private int amount;

    public WithdrawalRequest(long userId, int amount) {
        this.userId = userId;
        this.amount = amount;

    }

    public long getUserId() {
        return userId;
    }

    public int getAmount() {
        return amount;
    }


}
