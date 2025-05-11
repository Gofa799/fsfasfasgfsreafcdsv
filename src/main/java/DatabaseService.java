

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

    public void addUserIfNotExists(long telegramId, String username, Long referrerId) {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (telegram_id, username, balance, referrer_id) " +
                             "VALUES (?, ?, 20, ?) " +
                             "ON CONFLICT (telegram_id) DO NOTHING")) {
            stmt.setLong(1, telegramId);
            stmt.setString(2, username);
            if (referrerId != null) {
                stmt.setLong(3, referrerId);
            } else {
                stmt.setNull(3, Types.BIGINT);
            }
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
            if (isFirstTask(telegramId)) {
                try (PreparedStatement refStmt = conn.prepareStatement("SELECT referrer_id FROM users WHERE telegram_id = ?")) {
                    refStmt.setLong(1, telegramId);
                    ResultSet rs = refStmt.executeQuery();
                    if (rs.next()) {
                        long referrer = rs.getLong("referrer_id");
                        if (referrer != 0) {
                            try (PreparedStatement bonusStmt = conn.prepareStatement("UPDATE users SET balance = balance + 5 WHERE telegram_id = ?")) {
                                bonusStmt.setLong(1, referrer);
                                bonusStmt.executeUpdate();
                            }
                        }
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Task> getAvailableTasks(long userId) {
        List<Task> tasks = new ArrayList<>();

        String sql = """
        SELECT * FROM tasks t
        WHERE t.current_completions < t.max_completions
        AND NOT EXISTS (
            SELECT 1 FROM user_tasks ut WHERE ut.user_id = ? AND ut.task_id = t.id
        )
    """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Task task = new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("reward"),
                        rs.getString("type"),
                        rs.getString("target")
                );
                tasks.add(task);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tasks;
    }
    private boolean isFirstTask(long userId) throws SQLException {
        try (PreparedStatement stmt = DriverManager.getConnection(url, user, password)
                .prepareStatement("SELECT COUNT(*) FROM task_submissions WHERE telegram_id = ?")) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) == 0;
        }
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
    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY id";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Task task = new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("reward"),
                        rs.getString("type"),
                        rs.getString("target")
                );
                tasks.add(task);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tasks;
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