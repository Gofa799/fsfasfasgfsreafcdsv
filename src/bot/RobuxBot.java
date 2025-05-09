package bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RobuxBot extends TelegramLongPollingBot {
    private final long ADMIN_ID = 8121028171L;
    private final DatabaseService db = new DatabaseService();
    private final Set<Long> authorizedAdmins = new HashSet<>();
    private final Map<Long, Integer> lastBotMessages = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new RobuxBot());
            System.out.println("🤖 RobuxBot запущен успешно.");
        } catch (TelegramApiException e) {
            System.err.println("Ошибка запуска бота:");
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "robuxloots_bot";
    }

    @Override
    public String getBotToken() {
        return "7605702613:AAGlAsVzVjkTaU0F1xqlEvd4cdkaF4G4fUU";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            int msgId = update.getMessage().getMessageId();
            String text = update.getMessage().getText();

            MessageUtils.deleteMessage(this, chatId, msgId);

            switch (text) {
                case "/start":
                    db.addUserIfNotExists(chatId);
                    MessageUtils.sendText(this, chatId, "Добро пожаловать!", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                    break;

                case "/login":
                    if (chatId == ADMIN_ID) {
                        authorizedAdmins.add(chatId);
                        MessageUtils.sendText(this, chatId, "✅ Вы вошли как админ.", KeyboardFactory.adminKeyboard(), null, lastBotMessages);
                    } else {
                        MessageUtils.sendText(this, chatId, "⛔ Доступ запрещён.", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                    }
                    break;

                case "💼 Личный кабинет":
                    int robux = db.getRobux(chatId);
                    int completed = db.getCompletedTasks(chatId);
                    String profile = "Робуксы: " + robux + "\nВыполнено заданий: " + completed;
                    MessageUtils.sendText(this, chatId, profile, KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                    break;

                case "📋 Задания":
                    List<Task> tasks = db.getAllTasks();
                    MessageUtils.sendText(this, chatId, "Выберите задание:", KeyboardFactory.taskKeyboard(tasks, 1, 6), null, lastBotMessages);
                    break;

                case "🛠 Админ-панель":
                case "📊 Отчёт":
                case "📥 Заявки на вывод":
                    if (authorizedAdmins.contains(chatId)) {
                        handleAdminCommands(chatId, text);
                    } else {
                        MessageUtils.sendText(this, chatId, "⛔ Команда недоступна.", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                    }
                    break;

                default:
                    MessageUtils.sendText(this, chatId, "🤔 Неизвестная команда.", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callback = update.getCallbackQuery();
            String data = callback.getData();
            long chatId = callback.getMessage().getChatId();
            int messageId = callback.getMessage().getMessageId();

            if (data.startsWith("tasks_prev_")) {
                int currentPage = Integer.parseInt(data.substring("tasks_prev_".length()));
                editTaskPage(chatId, messageId, db.getAllTasks(), currentPage - 1);
            } else if (data.startsWith("tasks_next_")) {
                int currentPage = Integer.parseInt(data.substring("tasks_next_".length()));
                editTaskPage(chatId, messageId, db.getAllTasks(), currentPage + 1);
            } else if (data.startsWith("task_")) {
                int taskIndex = Integer.parseInt(data.substring("task_".length()));
                Task task = db.getAllTasks().get(taskIndex);
                MessageUtils.sendText(this, chatId, "📝 " + task.getTitle() + "\n\n" + task.getDescription(), KeyboardFactory.mainKeyboard(), null, lastBotMessages);
            } else if (data.startsWith("withdrawals_prev_")) {
                int currentPage = Integer.parseInt(data.substring("withdrawals_prev_".length()));
                editWithdrawalPage(chatId, messageId, db.getAllWithdrawalRequests(), currentPage - 1);
            } else if (data.startsWith("withdrawals_next_")) {
                int currentPage = Integer.parseInt(data.substring("withdrawals_next_".length()));
                editWithdrawalPage(chatId, messageId, db.getAllWithdrawalRequests(), currentPage + 1);
            } else if (data.startsWith("withdrawal_")) {
                int index = Integer.parseInt(data.substring("withdrawal_".length()));
                WithdrawalRequest req = db.getAllWithdrawalRequests().get(index);
                String info = "👤 Пользователь: " + req.getUserId() +
                        "\n💰 Сумма: " + req.getAmount() +
                        "\n📅 Дата: " + req.getDate();
                MessageUtils.sendText(this, chatId, info, KeyboardFactory.adminKeyboard(), null, lastBotMessages);
            }
        }
    }

    private void editTaskPage(long chatId, int messageId, List<Task> tasks, int page) {
        try {
            EditMessageText edit = new EditMessageText();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(messageId);
            edit.setText("Выберите задание:");
            edit.setReplyMarkup(KeyboardFactory.taskKeyboard(tasks, page, 6));
            execute(edit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editWithdrawalPage(long chatId, int messageId, List<WithdrawalRequest> requests, int page) {
        try {
            EditMessageText edit = new EditMessageText();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(messageId);
            edit.setText("📥 Заявки на вывод:");
            edit.setReplyMarkup(KeyboardFactory.withdrawalKeyboard(requests, page, 6));
            execute(edit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAdminCommands(long chatId, String command) {
        switch (command) {
            case "📊 Отчёт":
                int users = db.countUsers();
                MessageUtils.sendText(this, chatId, "👥 Всего пользователей: " + users, KeyboardFactory.adminKeyboard(), null, lastBotMessages);
                break;
            case "📥 Заявки на вывод":
                List<WithdrawalRequest> requests = db.getAllWithdrawalRequests();
                MessageUtils.sendText(this, chatId, "📥 Заявки на вывод:",
                        KeyboardFactory.withdrawalKeyboard(requests, 1, 6), // инлайн-клавиатура
                        KeyboardFactory.adminKeyboard(), // обычная клавиатура
                        lastBotMessages);
                break;
        }
    }
}
