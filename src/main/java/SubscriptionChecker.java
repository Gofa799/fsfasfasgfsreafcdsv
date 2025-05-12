import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;

import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SubscriptionChecker {

    // Метод проверки подписки
    public static boolean isSubscribed(AbsSender bot, long userId, String channelUsername) {
        try {
            GetChatMember getChatMember = new GetChatMember();
            getChatMember.setChatId("@" + channelUsername);
            getChatMember.setUserId(userId);

            ChatMember chatMember = bot.execute(getChatMember);
            String status = chatMember.getStatus();

            // Проверяем, что пользователь не "left"
            return !status.equals("left");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}