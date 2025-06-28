import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;

import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class SubscriptionChecker {

    public static boolean isSubscribed(AbsSender bot, long userId, String channelUsername) {
        try {
            GetChatMember getChatMember = new GetChatMember();
            getChatMember.setChatId(
                    channelUsername.startsWith("@") ? channelUsername : "@" + channelUsername
            );
            getChatMember.setUserId(userId);

            ChatMember chatMember = bot.execute(getChatMember);
            String status = chatMember.getStatus();

            return !status.equals("left");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}