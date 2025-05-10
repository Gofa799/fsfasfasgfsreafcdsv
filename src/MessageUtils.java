import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.util.Map;

public class MessageUtils {

    public static void sendText(TelegramLongPollingBot bot, long chatId, String text,
                                ReplyKeyboard replyKeyboard,
                                ReplyKeyboard inlineKeyboard,
                                Map<Long, Integer> lastBotMessages) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        if (inlineKeyboard != null) {
            message.setReplyMarkup(inlineKeyboard);
        } else if (replyKeyboard != null) {
            message.setReplyMarkup(replyKeyboard);
        }

        try {
            Message sentMessage = bot.execute(message);
            if (lastBotMessages.containsKey(chatId)) {
                int previousMsgId = lastBotMessages.get(chatId);
                try {
                    DeleteMessage delete = new DeleteMessage(String.valueOf(chatId), previousMsgId);
                    bot.execute(delete);
                } catch (Exception ignore) {
                }
            }
            lastBotMessages.put(chatId, sentMessage.getMessageId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteMessage(TelegramLongPollingBot bot, long chatId, int messageId) {
        try {
            DeleteMessage delete = new DeleteMessage(String.valueOf(chatId), messageId);
            bot.execute(delete);
        } catch (Exception e) {

        }
    }
}