import java.util.List;

public class SubgramTask {
    private final long telegramId;
    private final List<String> link;


    public SubgramTask(long telegramId, List<String> link, String status, String type) {
        this.telegramId = telegramId;
        this.link = link;
    }

    public long getTelegramId() {
        return telegramId;
    }

    public List<String> getLink() {
        return link;
    }

}