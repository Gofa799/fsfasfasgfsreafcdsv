public class SubgramTask {
    private final long telegramId;
    private final String link;
    private final String status;
    private final String type;

    public SubgramTask(long telegramId, String link, String status, String type) {
        this.telegramId = telegramId;
        this.link = link;
        this.status = status;
        this.type = type;
    }

    public long getTelegramId() {
        return telegramId;
    }

    public String getLink() {
        return link;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }
}