package application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;
import java.util.Scanner;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;


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

            uploadToBucket(storage, audioFile);
            playAudioFromBucketWithBackoff(storage, audioFile);
            cleanPromptFileFromBucket(storage, audioFile);

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

    private static void uploadToBucket(Storage storage, String filePath) {
        try {
            File file = new File(filePath);
            String key = file.getName();

            Blob blob = storage.get(PROMPTS_BUCKET, key);
            if (blob != null) {
                System.out.println("File already exists in the bucket. Skipping upload.");
                file.delete();
                return;
            }

            storage.create(Blob.newBuilder(PROMPTS_BUCKET, key).build(), Files.readAllBytes(file.toPath()));
            file.delete();
        } catch (Exception e) {
            System.out.println("Error uploading file to bucket: " + e.getMessage());
        }
    }

    private static void playAudioFromBucketWithBackoff(Storage storage, String key) {
        int maxRetries = 5;
        int initialDelay = 10;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Blob blob = storage.get(RESPONSES_BUCKET, key);
                if (blob != null) {
                    System.out.println("Playing audio response...");

                    File tempFile = File.createTempFile("temp", ".mp3");
                    blob.downloadTo(tempFile.toPath());

                    playAudio(tempFile.getAbsolutePath());

                    tempFile.delete();
                    blob.delete();

                    System.out.println("Finished playing audio response!");
                    break;
                } else {
                    System.out.println("No audio file found in the bucket");
                    if (attempt < maxRetries) {
                        int sleepTime = initialDelay * (int) Math.pow(2, attempt);
                        System.out.println("Retrying in " + sleepTime + " seconds...");
                        Thread.sleep(sleepTime * 1000);
                    } else {
                        System.out.println("Reached the maximum number of retries. Giving up.");
                    }
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                if (attempt < maxRetries) {
                    int sleepTime = initialDelay * (int) Math.pow(2, attempt);
                    System.out.println("Retrying in " + sleepTime + " seconds...");
                    try {
                        Thread.sleep(sleepTime * 1000);
                    } catch (InterruptedException ex) {
                        System.out.println("InterruptedException occurred: " + ex.getMessage());
                    }
                } else {
                    System.out.println("Reached the maximum number of retries. Giving up.");
                    break;
                }
            }
        }
    }

    private static void cleanPromptFileFromBucket(Storage storage, String key) {
        try {
            storage.delete(PROMPTS_BUCKET, String.format("prompts/%s", key));
            System.out.printf("Object '%s' successfully deleted from bucket '%s'%n", key, PROMPTS_BUCKET);
        } catch (Exception e) {
            System.out.printf("Error deleting object '%s' from bucket '%s': %s%n", key, PROMPTS_BUCKET, e.getMessage());
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
}
