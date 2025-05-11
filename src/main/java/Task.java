public class Task {
    private final int id;
    private final String title;
    private final String description;
    private final int reward;
    private final String type;

    public Task(int id, String title, String description, int reward, String type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.reward = reward;
        this.type = type;
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