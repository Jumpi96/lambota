package clients;

import com.google.api.client.json.Json;
import com.google.gson.*;
import entities.Conversation;
import entities.Message;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class OpenAIClient {

    private static final String API_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String ROLE = "role";
    private static final String CONTENT = "content";

    public static String askOpenAI(String apiKey, Conversation conversation) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("model", "gpt-3.5-turbo");
            JsonArray messages = new JsonArray();
            for (Message message: conversation.getMessages()) {
                JsonObject messageObject = new JsonObject();
                messageObject.addProperty(ROLE, message.getRole());
                messageObject.addProperty(CONTENT, message.getContent());
                messages.add(messageObject);
            }
            data.add("messages", messages);

            Gson gson = new Gson();
            String requestData = gson.toJson(data);

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestData, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            return responseJson.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
        } catch (Exception e) {
            System.out.println("Error occurred while querying OpenAI: " + e.getMessage());
            return "";
        }
    }
}
