package clients;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CloudStorageClient {

    private Storage storage;
    public CloudStorageClient() throws IOException {
        storage = StorageOptions.newBuilder()
            .setCredentials(GoogleCredentials.fromStream(CloudStorageClient.class.getResourceAsStream("/config/gcp-key.json")))
            .build()
            .getService();
    }

    public String uploadToBucket(String bucket, String filePath, boolean delete) {
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
            if (delete) {
                file.delete();
            }
            return String.format("gs://%s/%s", bucket, key);
        } catch (Exception e) {
            System.out.println("Error uploading file to bucket: " + e.getMessage());
        }
        return "";
    }

    public void cleanFileFromBucket(String bucket, String key) {
        try {
            storage.delete(bucket, key);
            System.out.printf("Object '%s' successfully deleted from bucket '%s'%n", key, bucket);
        } catch (Exception e) {
            System.out.printf("Error deleting object '%s' from bucket '%s': %s%n", key, bucket, e.getMessage());
        }
    }
}
