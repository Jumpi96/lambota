package audio;

import java.io.IOException;

public class AudioPlayer {
    public static void playAudio(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("afplay", filePath);
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error occurred while playing audio: " + e.getMessage());
        }
    }
}
