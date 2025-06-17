public class Submission {
    private final long userId;
    private final long taskId;
    private final String channel;
    private final double reward;

    public Submission(long userId, long taskId, String channel, double reward) {
        this.userId = userId;
        this.taskId = taskId;
        this.channel = channel;
        this.reward = reward;
    }

    public long getUserId() {
        return userId;
    }

    public long getTaskId() {
        return taskId;
    }

    public String getChannel() {
        return channel;
    }

    public double getReward() {
        return reward;
    }
}
