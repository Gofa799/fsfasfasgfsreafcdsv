import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
            con.setRequestProperty("Auth", token);
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

            if (json.has("status") && json.get("status").toString().equals("success")) {
                JSONObject additional = json.optJSONObject("additional");
                if (additional != null && additional.has("sponsors")) {
                    JSONArray sponsors = additional.getJSONArray("sponsors");

                    for (int i = 0; i < sponsors.length(); i++) {
                        JSONObject sponsor = sponsors.getJSONObject(i);

                        String link = sponsor.optString("link", null);
                        String status = sponsor.optString("status", "unknown");
                        String type = sponsor.optString("type", "unknown");

                        if (link != null && !link.isEmpty()) {
                            return new SubgramTask(user.getTelegramId(), link, status, type);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Ошибка при запросе задания из Subgram:");
            e.printStackTrace();
        }

        return null;
    }


    public boolean checkSubscription(long telegramId, String channelLink) {
        try {
            String urlStr = "https://api.subgram.ru/check-subscription";

            JSONObject body = new JSONObject();
            body.put("telegram_id", telegramId);
            body.put("links", new JSONArray().put(channelLink));

            JSONObject response = sendPost(urlStr, body);

            if (response.has("additional")) {
                JSONArray sponsors = response.getJSONObject("additional").optJSONArray("sponsors");
                if (sponsors != null) {
                    for (int i = 0; i < sponsors.length(); i++) {
                        JSONObject sponsor = sponsors.getJSONObject(i);
                        String link = sponsor.getString("link");
                        if (link.equals(channelLink)) {
                            return sponsor.getString("status").equals("subscribed");
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Ошибка при проверке подписки:");
            e.printStackTrace();
        }

        return false;
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

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) response.append(line);
        in.close();

        return new JSONObject(response.toString());
    }
}