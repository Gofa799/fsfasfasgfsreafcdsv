import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseService() {
        this.url = System.getenv("DB_URL");
        this.user = System.getenv("DB_USER");
        this.password = System.getenv("DB_PASSWORD");

    }

    public void addUserIfNotExists(long telegramId, String username) {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (telegram_id, username, balance) " +
                             "VALUES (?, ?, 20) " +
                             "ON CONFLICT (telegram_id) DO NOTHING")) {
            stmt.setLong(1, telegramId);
            stmt.setString(2, username);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public boolean submitTask(long telegramId, long taskId) {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT 1 FROM task_submissions WHERE telegram_id = ? AND task_id = ?")) {
                checkStmt.setLong(1, telegramId);
                checkStmt.setLong(2, taskId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) return false; // Уже выполнял
            }
            double reward = 0;
            try (PreparedStatement rewardStmt = conn.prepareStatement(
                    "SELECT reward FROM tasks WHERE id = ?")) {
                rewardStmt.setLong(1, taskId);
                ResultSet rs = rewardStmt.executeQuery();
                if (rs.next()) reward = rs.getDouble(1);
                else return false;
            }

            try (PreparedStatement updateBalance = conn.prepareStatement(
                    "UPDATE users SET balance = balance + ? WHERE telegram_id = ?")) {
                updateBalance.setDouble(1, reward);
                updateBalance.setLong(2, telegramId);
                updateBalance.executeUpdate();
            }

            try (PreparedStatement insertSubmission = conn.prepareStatement(
                    "INSERT INTO task_submissions (telegram_id, task_id, status) VALUES (?, ?, 'approved')")) {
                insertSubmission.setLong(1, telegramId);
                insertSubmission.setLong(2, taskId);
                insertSubmission.executeUpdate();
            }

            try (PreparedStatement updateCompletions = conn.prepareStatement(
                    "UPDATE tasks SET current_completions = current_completions + 1 WHERE id = ?")) {
                updateCompletions.setLong(1, taskId);
                updateCompletions.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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
        return getInt("balance", id); // или "balance", если у тебя поле так называется
    }

    public int getCompletedTasks(long id) {
        return getInt("completed", id);
    }

    private int getInt(String column, long id) {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement("SELECT " + column + " FROM users WHERE telegram_id = ?")) {
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