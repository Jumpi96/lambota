package gui;

import audio.AudioPlayer;
import controllers.ConversationController;
import entities.Conversation;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class Main {
    private JPanel rootPanel;
    private JButton spreektButton;
    private JButton repeatButton;
    private JTextArea writtenArea;
    private JButton showWrittenButton;
    private JTextField contextText;

    private ConversationController conversationController;
    private Conversation currentConversation;

    private boolean isRecording;

    public Main() throws IOException {
        conversationController = new ConversationController();

        writtenArea.setVisible(false);
        repeatButton.setEnabled(false);

        isRecording = false;
        spreektButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                contextText.setEnabled(false);
                if (currentConversation == null) {
                    currentConversation = new Conversation(0, contextText.getText());
                }
                if (!isRecording) {
                    conversationController.startRecording(currentConversation);
                    isRecording = true;
                    spreektButton.setText("Klik om te stoppen");
                } else {
                    conversationController.finishRecording(currentConversation);
                    repeatButton.setEnabled(true);
                    setWrittenArea(
                            currentConversation.getLastMessageContent(Conversation.USER),
                            currentConversation.getLastMessageContent(Conversation.ASSISTANT)
                    );
                    spreektButton.setText("Spreekt");
                    isRecording = false;
                }
            }
        });
        repeatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new AudioPlayer().playAudio(conversationController.getResponseFile());
            }
        });
        showWrittenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                writtenArea.setVisible(!writtenArea.isVisible());
            }
        });
    }

    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame("Main");
        frame.setContentPane(new Main().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void setWrittenArea(String prompt, String response) {
        writtenArea.setText(String.format("Jij: %s\n\nLambota: %s", prompt, response));
    }

}
