

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
        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            conn.setAutoCommit(false);

            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO users (telegram_id, username, balance, referrer_id) " +
                            "VALUES (?, ?, 0, ?) " +
                            "ON CONFLICT (telegram_id) DO NOTHING")) {
                insertStmt.setLong(1, telegramId);
                insertStmt.setString(2, username);
                if (referrerId != null) {
                    insertStmt.setLong(3, referrerId);
                } else {
                    insertStmt.setNull(3, Types.BIGINT);
                }

                int affectedRows = insertStmt.executeUpdate();

                if (affectedRows > 0 && referrerId != null) {
                    try (PreparedStatement rewardStmt = conn.prepareStatement(
                            "UPDATE users SET balance = balance + 5, referrers = referrers + 1 WHERE telegram_id = ?")) {
                        rewardStmt.setLong(1, referrerId);
                        rewardStmt.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public List<Submission> getAllSubscribeSubmissions() {
        List<Submission> submissions = new ArrayList<>();

        String query = """
        SELECT ut.telegram_id, ut.task_id, t.channel_username, t.reward
        FROM user_tasks ut
        JOIN tasks t ON t.id = ut.task_id
        WHERE t.type = 'subscribe'
    """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                long userId = rs.getLong("telegram_id");
                long taskId = rs.getLong("task_id");
                String channel = rs.getString("channel_username");
                double reward = rs.getDouble("reward");

                submissions.add(new Submission(userId, taskId, channel, reward));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return submissions;
    }
    public void removeTaskSubmission(long userId, long taskId) {
        String query = "DELETE FROM user_tasks WHERE telegram_id = ? AND task_id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, taskId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void deductBalance(long userId, double amount) {
        String query = "UPDATE users SET balance = balance - ? WHERE telegram_id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDouble(1, amount);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void decrementTaskCompletions(long taskId) {
        String query = "UPDATE tasks SET current_completions = current_completions - 1 WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, taskId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public boolean submitTask(long telegramId, long taskId) {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT 1 FROM user_tasks WHERE telegram_id = ? AND task_id = ?")) {
                checkStmt.setLong(1, telegramId);
                checkStmt.setLong(2, taskId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) return false; // Уже выполнял
            }
            double reward = 0;
            try (PreparedStatement rewardStmt = conn.prepareStatement(
                    "SELECT reward FROM tasks WHERE task_id = ?")) {
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
            try (PreparedStatement updateCompleted = conn.prepareStatement(
                    "UPDATE users SET completed = completed + 1 WHERE telegram_id = ?")) {
                updateCompleted.setLong(1, telegramId);
                updateCompleted.executeUpdate();
            }

            try (PreparedStatement insertSubmission = conn.prepareStatement(
                    "INSERT INTO user_tasks (telegram_id, task_id) VALUES (?, ?)")) {
                insertSubmission.setLong(1, telegramId);
                insertSubmission.setLong(2, taskId);
                insertSubmission.executeUpdate();
            }

            try (PreparedStatement updateCompletions = conn.prepareStatement(
                    "UPDATE tasks SET current_completions = current_completions + 1 WHERE task_id = ?")) {
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

    public List<Task> getAvailableTasks(long userId) {
        List<Task> tasks = new ArrayList<>();

        String sql = """
        SELECT * FROM tasks t
        WHERE t.current_completions < t.max_completions
        AND NOT EXISTS (
            SELECT 1 FROM user_tasks ut WHERE ut.telegram_id = ? AND ut.task_id = t.task_id
        )
    """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Task task = new Task(
                        rs.getInt("task_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("reward"),
                        rs.getString("type"),
                        rs.getString("channel_username")
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
                .prepareStatement("SELECT COUNT(*) FROM user_tasks WHERE telegram_id = ?")) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    public int getRobux(long id) {
        return getInt("balance", id);
    }
    public int getRef(long id) {
        return getInt("referrers", id);
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
    public List<Long> getAllUserIds() {
        List<Long> ids = new ArrayList<>();
        String sql = "SELECT telegram_id FROM users";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ids.add(rs.getLong("telegram_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ids;
    }

    public List<WithdrawalRequest> getAllWithdrawalRequests() {
        List<WithdrawalRequest> requests = new ArrayList<>();
        String sql = "SELECT roblox_username, amount FROM withdrawals ORDER BY date DESC";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                long userId = rs.getLong("roblox_username");
                int amount = rs.getInt("amount");


                WithdrawalRequest request = new WithdrawalRequest(userId, amount);
                requests.add(request);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return requests;
    }
    public boolean addWithdrawalRequest(long telegramId, double amount, String robloxUsername) {
        String query = """
        INSERT INTO withdraw_requests (telegram_id, amount, roblox_username)
        VALUES (?, ?, ?)
        ON CONFLICT (telegram_id)
        DO UPDATE SET 
            amount = EXCLUDED.amount,
            roblox_username = EXCLUDED.roblox_username;
    """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setLong(1, telegramId);
            ps.setDouble(2, amount);
            ps.setString(3, robloxUsername);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}