package bot;

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
        int totalPages = (int) Math.ceil((double) tasks.size() / pageSize);
        page = Math.max(1, Math.min(page, totalPages));

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, tasks.size());

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

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

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
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