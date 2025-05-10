package bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
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
}
