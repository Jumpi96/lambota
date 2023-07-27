package gui;

import audio.AudioPlayer;
import clients.CloudStorageClient;
import clients.OpenAIClient;
import clients.SpeechToTextClient;
import clients.TextToSpeechClient;
import utils.HashGenerator;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class Main {
    private JPanel rootPanel;
    private JButton spreektButton;
    private String audioFile;
    private TargetDataLine targetDataLine;
    private static final String PROMPTS_BUCKET = "lambota-audio-prompts";
    private static final String RESPONSES_BUCKET = "lambota-audio-responses";

    private Properties gcp;
    private CloudStorageClient cloudStorageClient;

    public Main() throws IOException {
        gcp = new Properties();
        gcp.load(Main.class.getResourceAsStream("/config/gcp.properties"));
        cloudStorageClient = new CloudStorageClient();
        spreektButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (audioFile == null) {
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                    String timestamp = now.format(formatter);
                    String shortHash = HashGenerator.generateShortHash();
                    audioFile = String.format("audio_%s_%s.mp3", timestamp, shortHash);
                    startRecording();
                    spreektButton.setText("Klik om te stoppen");
                } else {
                    stopRecording();
                    String uri = cloudStorageClient.uploadToBucket(PROMPTS_BUCKET, audioFile, false);
                    String transcription = new SpeechToTextClient().transcribeAudio(uri);
                    String response = new OpenAIClient().askOpenAI(gcp.getProperty("openai_key"), transcription);
                    String responsePath = new TextToSpeechClient()
                            .synthesizeSpeechAndSaveToFile(response, String.format("res_%s", audioFile));
                    new AudioPlayer().playAudio(responsePath);
                    cloudStorageClient.uploadToBucket(RESPONSES_BUCKET, audioFile, true);
                    spreektButton.setText("Spreekt");
                    audioFile = null;
                }
            }
        });
    }

    private void startRecording() {
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

    private void stopRecording() {
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
            System.out.println(String.format("Recording stopped and audio saved as %s", audioFile));
        }
    }

    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame("Main");
        frame.setContentPane(new Main().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

}
