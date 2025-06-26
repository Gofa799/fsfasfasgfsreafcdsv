import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SubgramClient {

    private final String token;

    public SubgramClient(String token) {
        this.token = token;
    }

    public SubgramTask getTask(User user, List<String> excludeChannels) {
        try {
            String urlStr = "https://api.subgram.ru/request-op/";
            URL url = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Auth", token); // передаём токен в заголовке
            con.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("UserId", String.valueOf(user.getTelegramId()));
            body.put("ChatId", String.valueOf(user.getTelegramId()));
            body.put("Gender", user.getSex());
            body.put("action", "subscribe");
            body.put("exclude_channel_ids", new JSONArray(excludeChannels));

            System.out.println("📤 Subgram request body: " + body);

            try (OutputStream os = con.getOutputStream()) {
                os.write(body.toString().getBytes());
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            System.out.println("📥 Subgram response: " + response);

            JSONObject json = new JSONObject(response.toString());

            JSONArray linksArray = json.optJSONArray("links");
            if (json.has("status") && linksArray != null && linksArray.length() > 0) {
                List<String> links = new ArrayList<>();
                for (int i = 0; i < linksArray.length(); i++) {
                    links.add(linksArray.getString(i));
                }

                // Пример: ставим 1 как награду (если reward нет в ответе — можно потом добавить)
                return new SubgramTask(
                        user.getTelegramId(),
                        links,
                        1,
                        json.optString("op_id", "unknown")
                );
            }

        } catch (Exception e) {
            System.out.println("❌ Ошибка при запросе задания из Subgram:");
            e.printStackTrace();
        }

        return null;
    }

    public boolean checkSubscription(long telegramId, String opId) {
        try {
            String urlStr = "https://api.subgram.ru/check-subscription";
            JSONObject body = new JSONObject();
            body.put("telegram_id", telegramId);
            body.put("op_id", opId);

            JSONObject response = sendPost(urlStr, body);
            return response.optBoolean("subscribed", false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean confirmSubscription(long telegramId, String opId) {
        try {
            String urlStr = "https://api.subgram.ru/confirm-subscription";
            JSONObject body = new JSONObject();
            body.put("telegram_id", telegramId);
            body.put("op_id", opId);

            JSONObject response = sendPost(urlStr, body);
            return response.optBoolean("success", false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private JSONObject sendGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream())
        );
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) response.append(line);
        in.close();
        return new JSONObject(response.toString());
    }

    private JSONObject sendPost(String urlStr, JSONObject body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Auth", token);
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            os.write(body.toString().getBytes());
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream())
        );
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) response.append(line);
        in.close();
        return new JSONObject(response.toString());
    }
}