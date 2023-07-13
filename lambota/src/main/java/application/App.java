package application;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;

import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.cloud.speech.v1p1beta1.*;

public class App {

    private static final String PROMPTS_BUCKET = "lambota-audio-prompts";
    private static final String RESPONSES_BUCKET = "lambota-audio-responses";

    public static void main(String[] args) throws IOException {
        Properties gcp = new Properties();
        gcp.load(App.class.getResourceAsStream("/config/gcp.properties"));

        System.out.println("Hi! I am your bot teacher. Let's start practicing. Record your message: (cut with CTRL+C)");

        Storage storage = StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(App.class.getResourceAsStream("/config/gcp-key.json")))
                .build()
                .getService();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String timestamp = now.format(formatter);
            String shortHash = generateShortHash();

            String audioFile = String.format("audio_%s_%s.mp3", timestamp, shortHash);
            recordAudio(audioFile);

            String uri = uploadToBucket(storage, PROMPTS_BUCKET, audioFile);
            String transcription = transcribeAudio(uri);
            String response = askOpenAI(gcp.getProperty("openai_key"), transcription);
            String responsePath = synthesizeSpeechAndSaveToFile(response, String.format("res_%s", audioFile));
            playAudio(responsePath);
            uploadToBucket(storage, RESPONSES_BUCKET, audioFile);

            System.out.println("Continue recording... (cut with CTRL+C)");
        }
    }

    private static void recordAudio(String filename) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("sox", "-d", "-t", "mp3", filename);
            Process process = processBuilder.start();
            Thread.sleep(5000);
            process.destroy();

            System.out.println("Recording complete!");
        } catch (IOException | InterruptedException e) {
            System.out.println("Error occurred while recording audio: " + e.getMessage());
        }
    }

    private static String uploadToBucket(Storage storage, String bucket, String filePath) {
        try {
            File file = new File(filePath);
            String key = file.getName();

            Blob blob = storage.get(bucket, key);
            if (blob != null) {
                System.out.println("File already exists in the bucket. Skipping upload.");
                file.delete();
                return "";
            }

            storage.create(Blob.newBuilder(bucket, key).build(), Files.readAllBytes(file.toPath()));
            file.delete();
            return String.format("gs://%s/%s", bucket, key);
        } catch (Exception e) {
            System.out.println("Error uploading file to bucket: " + e.getMessage());
        }
        return "";
    }

    private static void cleanPromptFileFromBucket(Storage storage, String bucket, String key) {
        try {
            storage.delete(bucket, String.format("prompts/%s", key));
            System.out.printf("Object '%s' successfully deleted from bucket '%s'%n", key, bucket);
        } catch (Exception e) {
            System.out.printf("Error deleting object '%s' from bucket '%s': %s%n", key, bucket, e.getMessage());
        }
    }

    private static String generateShortHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] randomBytes = new byte[8];
            md.update(randomBytes);
            byte[] hashBytes = md.digest();
            String encoded = Base64.getEncoder().encodeToString(hashBytes);
            return encoded.substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error occurred while generating short hash: " + e.getMessage());
            return "";
        }
    }

    private static void playAudio(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("afplay", filePath);
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error occurred while playing audio: " + e.getMessage());
        }
    }

    private static String transcribeAudio(String gcsUri) {
        try {
            SpeechClient client = SpeechClient.create();


            RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.MP3)
                    .setLanguageCode("nl-NL")
                    .build();

            LongRunningRecognizeResponse response = client.longRunningRecognizeAsync(config, audio).get();

            StringBuilder transcript = new StringBuilder();
            for (SpeechRecognitionResult result : response.getResultsList()) {
                for (SpeechRecognitionAlternative alternative : result.getAlternativesList()) {
                    transcript.append(alternative.getTranscript()).append(" ");
                }
            }
            return transcript.toString().trim();
        } catch (Exception e) {
            System.out.println("Error transcribing audio: " + e.getMessage());
            return "";
        }
    }

    private static String askOpenAI(String apiKey, String inputPrompt) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("model", "text-davinci-003");
            data.addProperty("prompt", "You're a Dutch teacher and you are practicing speaking with a student. Your answer should only be what the Dutch teacher answers. The student says: " + inputPrompt);
            data.addProperty("max_tokens", 100);

            Gson gson = new Gson();
            String requestData = gson.toJson(data);

            String apiEndpoint = "https://api.openai.com/v1/completions";
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiEndpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestData, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            System.out.println(responseBody);

            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            return responseJson.getAsJsonArray("choices").get(0).getAsJsonObject().get("text").getAsString();
        } catch (Exception e) {
            System.out.println("Error occurred while querying OpenAI: " + e.getMessage());
            return "";
        }
    }

    public static String synthesizeSpeechAndSaveToFile(String text, String filePath) {
        try {
            TextToSpeechClient client = TextToSpeechClient.create();

            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("nl-NL")
                    .setName("nl-NL-Wavenet-B")
                    .build();

            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .build();

            SynthesizeSpeechResponse response = client.synthesizeSpeech(input, voice, audioConfig);

            byte[] audioContent = response.getAudioContent().toByteArray();

            Path path = Path.of(filePath);
            Files.write(path, audioContent, StandardOpenOption.CREATE);

            System.out.println("Audio file saved to: " + filePath.toString());
            return path.toString();
        } catch (Exception e) {
            System.out.println("Error synthesizing speech and uploading to bucket: " + e.getMessage());
        }
        return null;
    }
}
