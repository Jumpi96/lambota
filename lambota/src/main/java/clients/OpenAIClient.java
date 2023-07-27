package clients;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class OpenAIClient {

    private static final String API_ENDPOINT = "https://api.openai.com/v1/completions";
    public static String askOpenAI(String apiKey, String inputPrompt) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("model", "text-davinci-003");
            data.addProperty("prompt", "You're a Dutch teacher and you are practicing speaking with a student. Your answer should only be what the Dutch teacher answers. The student says: " + inputPrompt);
            data.addProperty("max_tokens", 100);

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
            return responseJson.getAsJsonArray("choices").get(0).getAsJsonObject().get("text").getAsString();
        } catch (Exception e) {
            System.out.println("Error occurred while querying OpenAI: " + e.getMessage());
            return "";
        }
    }
}
