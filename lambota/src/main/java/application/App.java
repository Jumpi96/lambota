package application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;
import java.util.Scanner;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class App {

    private static final String BUCKET_NAME = "lambota-polly-transcribe";

    public static void main(String[] args) throws IOException {
        Properties aws = new Properties();
        aws.load(App.class.getResourceAsStream("/config/aws.properties"));

        System.out.println("Hi! I am your bot teacher. Let's start practicing. Record your message: (cut with CTRL+C)");
        AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials((String) aws.get("access_key_id"), (String) aws.get("secret_access_key")));

        Scanner scanner = new Scanner(System.in);

        while (true) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String timestamp = now.format(formatter);
            String shortHash = generateShortHash();

            String audioFile = String.format("audio_%s_%s.mp3", timestamp, shortHash);
            recordAudio(audioFile);

            uploadToS3(s3, audioFile);
            playAudioFromS3WithBackoff(s3, String.format("response/%s", audioFile));
            cleanPromptFileFromS3(s3, audioFile);

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

    private static void uploadToS3(AmazonS3 s3, String filePath) {
        try {
            File file = new File(filePath);
            String key = String.format("prompts/%s", file.getName());

            PutObjectRequest request = new PutObjectRequest(BUCKET_NAME, key, file);
            s3.putObject(request);
            file.delete();
        } catch (Exception e) {
            System.out.println("Error uploading file to S3: " + e.getMessage());
        }
    }

    private static void playAudioFromS3WithBackoff(AmazonS3 s3, String key) {
        int maxRetries = 5;
        int initialDelay = 10;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                ObjectMetadata objectMetadata = s3.getObjectMetadata(BUCKET_NAME, key);
                if (objectMetadata != null) {
                    System.out.println("Playing audio response...");

                    S3Object s3Object = s3.getObject(BUCKET_NAME, key);
                    S3ObjectInputStream inputStream = s3Object.getObjectContent();
                    File tempFile = File.createTempFile("temp", ".mp3");

                    Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    playAudio(tempFile.getAbsolutePath());

                    tempFile.delete();
                    inputStream.close();
                    s3Object.close();

                    System.out.println("Finished playing audio response!");
                    break;
                } else {
                    System.out.println("No audio file found in the S3 bucket");
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

    private static void cleanPromptFileFromS3(AmazonS3 s3, String key) {
        try {
            s3.deleteObject(BUCKET_NAME, String.format("prompts/%s", key));
            System.out.printf("Object '%s' successfully deleted from bucket '%s'%n", key, BUCKET_NAME);
        } catch (Exception e) {
            System.out.printf("Error deleting object '%s' from bucket '%s': %s%n", key, BUCKET_NAME, e.getMessage());
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
