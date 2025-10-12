package sevengtz.autochatmod;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {
    private final HttpClient httpClient;

    public DiscordWebhook() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendMessage(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();

        if (config.webhookUrl.isEmpty() || config.webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_URL_HERE")) {
            AutoChatMod.LOGGER.warn("Discord webhook URL not configured");
            return;
        }

        String finalMessage = message;
        if (config.enableDiscordPing && !config.userMentionId.equals("YOUR ID HERE")) {
            finalMessage += "\n<@" + config.userMentionId + ">";
        }

        sendMessageAsync(finalMessage);
    }

    private void sendMessageAsync(String message) {
        CompletableFuture.runAsync(() -> {
            try {
                ConfigManager.Config config = ConfigManager.getConfig();
                JsonObject payload = new JsonObject();
                payload.addProperty("content", message);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200 && response.statusCode() != 204) {
                    AutoChatMod.LOGGER.warn("Failed to send Discord message. Status: {}",
                            response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                AutoChatMod.LOGGER.error("Error sending Discord message", e);
            }
        });
    }
}