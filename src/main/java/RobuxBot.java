import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RobuxBot extends TelegramLongPollingBot {
    private final long ADMIN_ID = 8121028171L;
    private final DatabaseService db = new DatabaseService();
    private final Set<Long> authorizedAdmins = new HashSet<>();
    private final Map<Long, Integer> lastBotMessages = new ConcurrentHashMap<>();



    @Override
    public String getBotUsername() {
        return System.getenv("BOT_USERNAME");
    }

    @Override
    public String getBotToken() {
        return System.getenv("BOT_TOKEN");
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
                    MessageUtils.sendText(this, chatId, "Добро пожаловать! RobuxLoot уникальный сервис для заработка робуксов! Для начала нажми кнопку Задания. ", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                    break;

                case "/login":
                    if (chatId == ADMIN_ID) {
                        authorizedAdmins.add(chatId);
                        MessageUtils.sendText(this, chatId, "✅ Вы вошли как админ.", KeyboardFactory.adminKeyboard(), null, lastBotMessages);
                    } else {
                        MessageUtils.sendText(this, chatId, "⛔ Доступ запрещён.", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                    }
                    break;
                case "❓ Помощь":
                    MessageUtils.sendText(this, chatId, "❓ Помощь — Как вывести валюту\n" +
                            "Чтобы подать заявку на вывод, выполните следующие шаги:\n" +
                            "\n" +
                            "Нажмите \"\uD83D\uDCBC Личный кабинет\", затем выберите \"\uD83E\uDD11 Вывод\".\n" +
                            "\n" +
                            "\uD83D\uDCACВ одном сообщении укажите:" +
                            "\n" +
                            "Ваш ник в Roblox." +
                            "\n" +
                            "Затем желаемую сумму вывода.\n" +
                            "\n" +
                            "После отправки заявки:" +
                            "\n" +
                            "Вы должны будете создать карту на указанном аккаунте с геймпассом и ценой которую выдал бот.\n" +
                            "\n" +
                            "\uD83D\uDCAC Важно:\n" +
                            "Выплата производится в течение 7 дней после запроса на вывод.", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                    break;

                case "💼 Личный кабинет":
                    int robux = db.getRobux(chatId);
                    int completed = db.getCompletedTasks(chatId);
                    String profile = "Робуксы: " + robux + "\nВыполнено заданий: " + completed;
                    MessageUtils.sendText(this, chatId, profile, KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                    break;

                case "📋 Задания":
                    List<Task> tasks = db.getAllTasks();
                    MessageUtils.sendText(this, chatId, "Доступные задания:", KeyboardFactory.taskKeyboard(tasks, 1, 6), null, lastBotMessages);
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
            edit.setText("Доступные задания:");
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
