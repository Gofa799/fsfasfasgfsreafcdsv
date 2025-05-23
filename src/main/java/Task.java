public class Task {
    private final int id;
    private final String title;
    private final String description;
    private final int reward;
    private final String type;
    private String channelUsername;

    public Task(int id, String title, String description, int reward, String type, String channel_username) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.reward = reward;
        this.type = type;
        this.channelUsername = channel_username;
    }
    public String getChannelUsername() {
        return channelUsername;
    }
    public void setChannelUsername(String channelUsername) {
        this.channelUsername = channelUsername;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getReward() {
        return reward;
    }

    public String getType() {
        return type;
    }

}