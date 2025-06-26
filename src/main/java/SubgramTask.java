import java.util.List;

public class SubgramTask {
    private final long telegramId;
    private final List<String> links;
    private final int reward;
    private final String opId;

    public SubgramTask(long telegramId, List<String> links, int reward, String opId) {
        this.telegramId = telegramId;
        this.links = links;
        this.reward = reward;
        this.opId = opId;
    }

    public List<String> getLinks() {
        return links;
    }

    public int getReward() {
        return reward;
    }

    public String getOpId() {
        return opId;
    }

    public long getTelegramId() {
        return telegramId;
    }
}
