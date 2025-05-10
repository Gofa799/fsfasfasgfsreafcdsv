import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load(); // Загружает переменные из .env
        this.url = dotenv.get("DB_URL");
        this.user = dotenv.get("DB_USER");
        this.password = dotenv.get("DB_PASSWORD");
        init();
    }

    public void init() {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT PRIMARY KEY, " +
                    "robux INT DEFAULT 0, " +
                    "completed INT DEFAULT 0)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addUserIfNotExists(long id) {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (id) VALUES (?) ON CONFLICT DO NOTHING")) {
            stmt.setLong(1, id);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Task> getAllTasks() {
        return List.of(
                new Task("Подпишись на канал 1", "Описание 1"),
                new Task("Подпишись на канал 2", "Описание 2"),
                new Task("Установи приложение", "Описание 3"),
                new Task("Сделай репост", "Описание 4"),
                new Task("Поставь лайк", "Описание 5"),
                new Task("Посети сайт", "Описание 6"),
                new Task("Скачай игру", "Описание 7")
        );
    }

    public int getRobux(long id) {
        return getInt("robux", id);
    }

    public int getCompletedTasks(long id) {
        return getInt("completed", id);
    }

    private int getInt(String column, long id) {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement("SELECT " + column + " FROM users WHERE id = ?")) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countUsers() {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<WithdrawalRequest> getAllWithdrawalRequests() {
        List<WithdrawalRequest> requests = new ArrayList<>();
        String sql = "SELECT user_id, amount, date FROM withdrawals ORDER BY date DESC";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                long userId = rs.getLong("user_id");
                int amount = rs.getInt("amount");
                String date = rs.getTimestamp("date").toString();

                WithdrawalRequest request = new WithdrawalRequest(userId, amount, date);
                requests.add(request);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return requests;
    }
}