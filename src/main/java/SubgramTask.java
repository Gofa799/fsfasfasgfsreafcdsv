public class SubgramTask {
    private final long telegramId;
    private final String link;

    public SubgramTask(long telegramId, String link) {
        this.telegramId = telegramId;
        this.link = link;
    }

    public long getTelegramId() {
        return telegramId;
    }

    public String getLink() {
        return link;
    }
}