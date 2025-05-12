import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {

    public static ReplyKeyboardMarkup mainKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("💼 Личный кабинет"));
        row1.add(new KeyboardButton("📋 Задания"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("❓ Помощь"));

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);
        return markup;
    }

    public static ReplyKeyboardMarkup adminKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📊 Отчёт"));
        row1.add(new KeyboardButton("📋 Задания"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("💼 Личный кабинет"));
        row2.add(new KeyboardButton("📥 Заявки на вывод"));

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);
        return markup;
    }
    public static InlineKeyboardMarkup taskKeyboard(List<Task> tasks, int page, int pageSize) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (tasks == null || tasks.isEmpty()) {
            InlineKeyboardButton refresh = new InlineKeyboardButton("🔄 Обновить список");
            refresh.setCallbackData("refresh_tasks");

            InlineKeyboardButton back = new InlineKeyboardButton("🔙 Назад");
            back.setCallbackData("back_to_menu");

            rows.add(List.of(refresh));
            rows.add(List.of(back));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(rows);
            return markup;
        }

        int totalPages = (int) Math.ceil((double) tasks.size() / pageSize);
        page = Math.max(1, Math.min(page, totalPages));

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, tasks.size());

        for (int i = start; i < end; i++) {
            Task task = tasks.get(i);
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(task.getTitle());
            button.setCallbackData("task_" + i);
            rows.add(List.of(button));
        }

        InlineKeyboardButton left = new InlineKeyboardButton("⬅️");
        left.setCallbackData("tasks_prev_" + page);

        InlineKeyboardButton right = new InlineKeyboardButton("➡️");
        right.setCallbackData("tasks_next_" + page);

        InlineKeyboardButton label = new InlineKeyboardButton("📄 " + page + " / " + totalPages);
        label.setCallbackData("noop");

        List<InlineKeyboardButton> navRow = new ArrayList<>();
        if (page > 1) navRow.add(left);
        navRow.add(label);
        if (page < totalPages) navRow.add(right);
        rows.add(navRow);

        InlineKeyboardButton back = new InlineKeyboardButton("🔙 Назад");
        back.setCallbackData("back_to_menu");
        rows.add(List.of(back));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    public static InlineKeyboardMarkup taskDetailsKeyboard(Task task) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if ("subscribe".equalsIgnoreCase(task.getType())) {
            InlineKeyboardButton checkButton = new InlineKeyboardButton("✅ Проверить задание");
            checkButton.setCallbackData("check_task_" + task.getId());
            rows.add(List.of(checkButton));
        }

        InlineKeyboardButton backButton = new InlineKeyboardButton("🔙 Назад");
        backButton.setCallbackData("back_to_tasks");
        rows.add(List.of(backButton));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    public static InlineKeyboardMarkup profileKeyboard() {
        InlineKeyboardButton withdrawButton = new InlineKeyboardButton();
        withdrawButton.setText("💸 Вывести");
        withdrawButton.setCallbackData("withdraw");

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Назад");
        backButton.setCallbackData("back_to_menu");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(withdrawButton),
                List.of(backButton)
        ));
        return markup;
    }
    public static InlineKeyboardMarkup withdrawalKeyboard(List<WithdrawalRequest> requests, int page, int itemsPerPage) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        int totalPages = (int) Math.ceil((double) requests.size() / itemsPerPage);

        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, requests.size());

        for (int i = start; i < end; i++) {
            WithdrawalRequest request = requests.get(i);
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("👤 " + request.getUserId() + " | 💰 " + request.getAmount());
            btn.setCallbackData("withdrawal_" + i);

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(btn);
            rows.add(row);
        }

        List<InlineKeyboardButton> navigationRow = new ArrayList<>();
        if (page > 1) {
            InlineKeyboardButton prevBtn = new InlineKeyboardButton("⬅️ Назад");
            prevBtn.setCallbackData("withdrawals_prev_" + page);
            navigationRow.add(prevBtn);
        }
        if (page < totalPages) {
            InlineKeyboardButton nextBtn = new InlineKeyboardButton("➡️ Далее");
            nextBtn.setCallbackData("withdrawals_next_" + page);
            navigationRow.add(nextBtn);
        }
        if (!navigationRow.isEmpty()) {
            rows.add(navigationRow);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
}