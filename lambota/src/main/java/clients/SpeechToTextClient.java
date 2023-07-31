package clients;

import com.google.cloud.speech.v1p1beta1.*;

public class SpeechToTextClient {

    private SpeechClient client;

    public SpeechToTextClient() {
        try {
            client = SpeechClient.create();
        } catch (Exception e) {
            System.out.println("Error creating SpeechClient: " + e.getMessage());
        }
    }
    public String transcribeAudio(String gcsUri) {
        try {
            RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

            RecognitionConfig config = RecognitionConfig.newBuilder()
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
}
