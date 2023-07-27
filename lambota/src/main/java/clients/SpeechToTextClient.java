package clients;

import com.google.cloud.speech.v1p1beta1.*;

public class SpeechToTextClient {
    public static String transcribeAudio(String gcsUri) {
        try {
            SpeechClient client = SpeechClient.create();

            RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    //.setEncoding(RecognitionConfig.AudioEncoding.MP3)
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
