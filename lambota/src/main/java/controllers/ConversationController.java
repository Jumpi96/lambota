package controllers;

import audio.AudioPlayer;
import clients.CloudStorageClient;
import clients.OpenAIClient;
import clients.SpeechToTextClient;
import clients.TextToSpeechClient;
import entities.Conversation;
import entities.Message;
import gui.Main;
import utils.HashGenerator;

import javax.sound.sampled.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class ConversationController {
    private String audioFile;
    private String responseFile;
    private TargetDataLine targetDataLine;
    private static final String PROMPTS_BUCKET = "lambota-audio-prompts";
    private static final String RESPONSES_BUCKET = "lambota-audio-responses";
    private Properties gcp;
    private CloudStorageClient cloudStorageClient;
    private SpeechToTextClient speechToTextClient;

    public ConversationController() {
        gcp = new Properties();
        try {
            gcp.load(Main.class.getResourceAsStream("/config/gcp.properties"));
            cloudStorageClient = new CloudStorageClient();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        speechToTextClient = new SpeechToTextClient();
    }

    public void startRecording(Conversation currentConversation) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = now.format(formatter);
        String shortHash = HashGenerator.generateShortHash();
        audioFile = String.format("audio_%s_%s.mp3", timestamp, shortHash);
        record();
    }

    private void record() {
        try {
            AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 1, 2, 48000, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            System.out.println("Recording started...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AudioInputStream audioInputStream = new AudioInputStream(targetDataLine);
                    try {
                        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new java.io.File(audioFile));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    public void finishRecording(Conversation conversation) {
        stopRecording();
        String uri = cloudStorageClient.uploadToBucket(PROMPTS_BUCKET, audioFile);
        String transcription = speechToTextClient.transcribeAudio(uri);
        conversation.addMessage(new Message(Conversation.USER, transcription));
        String response = new OpenAIClient().askOpenAI(gcp.getProperty("openai_key"), conversation);
        conversation.addMessage(new Message(Conversation.ASSISTANT, response));
        responseFile = new TextToSpeechClient()
                .synthesizeSpeechAndSaveToFile(response, String.format("res_%s", audioFile));

        new AudioPlayer().playAudio(responseFile);
        cloudStorageClient.uploadToBucket(RESPONSES_BUCKET, audioFile);
        audioFile = null;
    }

    private void stopRecording() {
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
            System.out.println(String.format("Recording stopped and audio saved as %s", audioFile));
        }
    }

    public String getResponseFile() {
        return responseFile;
    }
}
