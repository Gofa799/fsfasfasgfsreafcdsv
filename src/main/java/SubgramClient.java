import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SubgramClient {

    private static final String API_URL = "https://subgram.ru/api/bot/";
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

            JSONObject json = new JSONObject(response.toString());
            if (json.optBoolean("success", false) && json.has("link")) {
                return new SubgramTask(
                        user.getTelegramId(),
                        json.getString("link"),
                        json.getInt("reward"),
                        json.getString("op_id")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean checkSubscription(long telegramId, String opId) {
        try {
            String url = API_URL + "check-subscription?telegram_id=" + telegramId + "&op_id=" + opId + "&token=" + token;
            JSONObject response = sendGet(url);
            return response.optBoolean("subscribed", false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean confirmSubscription(long telegramId, String opId) {
        try {
            String url = API_URL + "confirm-subscription";
            JSONObject body = new JSONObject();
            body.put("telegram_id", telegramId);
            body.put("op_id", opId);
            body.put("token", token);
            JSONObject response = sendPost(url, body);
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