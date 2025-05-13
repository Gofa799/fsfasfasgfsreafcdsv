import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RobuxBot extends TelegramLongPollingBot {
    private final long ADMIN_ID = 8121028171L;
    private final DatabaseService db = new DatabaseService();
    private final Set<Long> authorizedAdmins = new HashSet<>();
    private final Map<Long, Integer> lastBotMessages = new ConcurrentHashMap<>();

    private final Map<Long, WithdrawState> withdrawStates = new HashMap<>();
    private final Set<Long> awaitingAmount = new HashSet<>();
    private final Set<Long> awaitingNickname = new HashSet<>();




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
            Message message = update.getMessage();
            long telegramId = message.getFrom().getId();
            String username = message.getFrom().getUserName();
            if (username == null) {
                username = "unknown";
            }
            String text = update.getMessage().getText();

            MessageUtils.deleteMessage(this, chatId, msgId);
            if (text.startsWith("/start")) {
                String[] parts = text.split(" ");
                Long referrerId = null;

                if (parts.length == 2) {
                    try {
                        referrerId = Long.parseLong(parts[1]);
                    } catch (NumberFormatException ignored) {}
                }

                db.addUserIfNotExists(telegramId, username, referrerId);

                MessageUtils.sendText(this, chatId,
                        "Добро пожаловать! RobuxLoot — уникальный сервис для заработка робуксов! Для начала нажми кнопку Задания.",
                        KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                return;
            }
            if (awaitingAmount.contains(telegramId)) {
                String amountText = text.trim();

                try {
                    int amount = Integer.parseInt(amountText);
                    int balance = db.getRobux(telegramId);
                    int referrers = db.getRef(telegramId);


                    if (amount < 100) {
                        MessageUtils.sendText(this, chatId, "❌ Сумма должна быть больше 100", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                        return;
                    }
                    if (amount > balance) {
                        MessageUtils.sendText(this, chatId, "❌ Недостаточно робуксов", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                        return;
                    }
                    if (referrers < 5) {
                        MessageUtils.sendText(this, chatId, "❌ Недостаточно рефералов, должно быть 5 и больше!", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                        return;
                    }

                    WithdrawState state = withdrawStates.getOrDefault(telegramId, new WithdrawState());
                    state.setAmount(amount);
                    state.setStage(1);
                    withdrawStates.put(telegramId, state);

                    MessageUtils.sendText(this, chatId, "✏️ Теперь введите ваш username в Roblox.", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                    awaitingAmount.remove(telegramId);
                    awaitingNickname.add(telegramId);

                } catch (NumberFormatException e) {
                awaitingAmount.remove(telegramId);
                MessageUtils.sendText(this, chatId, "❌ Введите корректное число.", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
            }

                return;
            }
            if (awaitingNickname.contains(telegramId)) {
                String nickname = text.trim();

                WithdrawState state = withdrawStates.getOrDefault(telegramId, null);
                if (state == null || state.getStage() != 1) {
                    MessageUtils.sendText(this, chatId, "❗ Что-то пошло не так. Попробуйте ещё раз позже.", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                    return;
                }

                state.setNickname(nickname);
                state.setStage(2);
                withdrawStates.put(telegramId, state);

                db.addWithdrawalRequest(telegramId, state.getAmount(), nickname);

                MessageUtils.sendText(this, chatId, "✅ Заявка на вывод отправлена!", KeyboardFactory.mainKeyboard(), null, lastBotMessages);

                awaitingNickname.remove(telegramId);
                return;
            }

            switch (text) {
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
                            "Нажмите \"\uD83D\uDCBC Личный кабинет\", затем выберите 💸 Вывести\".\n" +
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
                    int robux = db.getRobux(telegramId);
                    int completed = db.getCompletedTasks(telegramId);
                    int referrers = db.getRef(telegramId);
                    String profile = "👤 Профиль: @" + (username != null ? username : "Без ника") +
                            "\n🆔 ID: " + telegramId +
                            "\n💰 Робуксы: " + robux +
                            "\n✅ Выполнено заданий: " + completed +
                            "\n👤 Рефералы: " + referrers +
                            "\n🔗 Ваша реферальная ссылка: https://t.me/" + getBotUsername() + "?start=" + telegramId;

                    MessageUtils.sendText(this, chatId, profile, KeyboardFactory.profileKeyboard(), null, lastBotMessages);
                    break;
                case "📋 Задания":
                    List<Task> tasks = db.getAvailableTasks(telegramId);
                    MessageUtils.sendText(this, chatId, "Доступные задания(скоро будет больше):", KeyboardFactory.taskKeyboard(tasks, 1, 6), null, lastBotMessages);
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
            long telegramId = callback.getFrom().getId();

            if (data.startsWith("tasks_prev_")) {
                int currentPage = Integer.parseInt(data.substring("tasks_prev_".length()));
                editTaskPage(chatId, messageId, db.getAvailableTasks(telegramId), currentPage - 1);
            } else if (data.startsWith("tasks_next_")) {
                int currentPage = Integer.parseInt(data.substring("tasks_next_".length()));
                editTaskPage(chatId, messageId, db.getAvailableTasks(telegramId), currentPage + 1);
            } else if (data.startsWith("task_")) {
                int taskIndex = Integer.parseInt(data.substring("task_".length()));
                Task task = db.getAvailableTasks(telegramId).get(taskIndex);
                InlineKeyboardMarkup keyboard = KeyboardFactory.taskDetailsKeyboard(task);
                String text = "📝 " + task.getTitle() + "\n\n" +
                        task.getDescription() + "\n\n" +
                        "💰 Награда: " + task.getReward() + "Робуксов";

                MessageUtils.sendText(this, chatId, text, keyboard, null, lastBotMessages);
            }
            else if (data.startsWith("check_task_")) {
            long taskId = Long.parseLong(data.substring("check_task_".length()));


            Task task = null;
            for (Task t : db.getAvailableTasks(telegramId)) {
                if (t.getId() == taskId) {
                    task = t;
                    break;
                }
            }

            if (task == null) {
                MessageUtils.sendText(this, chatId, "❗ Задание не найдено.", KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                return;
            }

            boolean success = false;


            if (task.getType().equals("subscribe")) {
                success = SubscriptionChecker.isSubscribed(this, telegramId, task.getChannelUsername());
            }


            if (success) {
                boolean submitted = db.submitTask(telegramId, taskId);
                if (submitted) {
                    MessageUtils.sendText(this, chatId,
                            "✅ Задание выполнено! Вам начислено " + task.getReward() + " робуксов.",
                            KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                } else {
                    MessageUtils.sendText(this, chatId,
                            "⚠️ Вы уже выполняли это задание.",
                            KeyboardFactory.mainKeyboard(), null, lastBotMessages);
                }
            } else {
                MessageUtils.sendText(this, chatId,
                        "❌ Вы ещё не выполнили задание. Пожалуйста, выполните его и попробуйте снова.",
                        KeyboardFactory.mainKeyboard(), null, lastBotMessages);
            }} else if (data.equals("withdraw_request")) {
                awaitingAmount.add(telegramId);
                withdrawStates.put(telegramId, new WithdrawState());
                MessageUtils.sendText(this, chatId, "💸 Введите сумму для вывода:",KeyboardFactory.mainKeyboard(),null, lastBotMessages);

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
                        "\n💰 Сумма: " + req.getAmount();
                MessageUtils.sendText(this, chatId, info, KeyboardFactory.adminKeyboard(), null, lastBotMessages);
            } else if (data.equals("back_to_menu")) {

            MessageUtils.sendText(this, chatId,
                    "Вы вернулись в главное меню.",
                    KeyboardFactory.mainKeyboard(), null, lastBotMessages);
            MessageUtils.deleteMessage(this, chatId, messageId);
        }
            else if (data.equals("back_to_tasks")) {

                List<Task> tasks = db.getAvailableTasks(telegramId);
                MessageUtils.sendText(this, chatId, "Доступные задания(скоро будет больше):", KeyboardFactory.taskKeyboard(tasks, 1, 6), null, lastBotMessages);
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
                        KeyboardFactory.withdrawalKeyboard(requests, 1, 6),
                        KeyboardFactory.adminKeyboard(),
                        lastBotMessages);
                break;
        }
    }
}
