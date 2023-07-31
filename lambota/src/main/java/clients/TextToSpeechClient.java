package clients;

import com.google.cloud.texttospeech.v1.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TextToSpeechClient {
    private com.google.cloud.texttospeech.v1.TextToSpeechClient client;

    public TextToSpeechClient() {
        try {
            client = com.google.cloud.texttospeech.v1.TextToSpeechClient.create();
        }
        catch (Exception e) {
            System.out.println("Error creating TextToSpeechClient: " + e.getMessage());
        }
    }
    public String synthesizeSpeechAndSaveToFile(String text, String filePath) {
        try {
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
