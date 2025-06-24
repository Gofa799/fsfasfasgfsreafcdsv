public class User {
    private long telegramId;
    private String username;
    private String sex;


    public long getTelegramId() {
        return telegramId;
    }

    public String getUsername() {
        return username;
    }

    public String getSex() {
        return sex;
    }


    public void setTelegramId(long telegramId) {
        this.telegramId = telegramId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}