package audio;

import java.io.IOException;

public class AudioRecorder {
    public static void recordAudio(String filename) {
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
}
