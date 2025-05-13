public class WithdrawState {
    private int amount;
    private String nickname;
    private int stage; // 0 - ждем сумму, 1 - ждем ник

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public int getStage() { return stage; }
    public void setStage(int stage) { this.stage = stage; }
}