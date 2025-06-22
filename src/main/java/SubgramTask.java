public class SubgramTask {
    private final long telegramId;
    private final String opId;
    private final String link;
    private final int reward;

    public SubgramTask(long telegramId, String link, int reward, String opId) {
        this.telegramId = telegramId;
        this.link = link;
        this.reward = reward;
        this.opId = opId;
    }

    public long getTelegramId() { return telegramId; }
    public String getOpId() { return opId; }
    public String getLink() { return link; }
    public int getReward() { return reward; }
}