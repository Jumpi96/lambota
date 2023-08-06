package entities;

import java.util.ArrayList;
import java.util.List;

public class Conversation {
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";
    public static final String SYSTEM = "system";

    private static final String BASIC_CONTEXT = "You're a Dutch teacher and you are practicing speaking with a student. Your responses should have a similar length to the student comments.";


    private Long id;
    private List<Message> messages;

    public Conversation(long id, String context) {
        this.id = id;
        this.messages = new ArrayList<>();
        this.messages.add(new Message(SYSTEM, String.format("%s %s", BASIC_CONTEXT, context)));
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public List<Message> getMessages() {
        return messages;
    }

    public String getLastMessageContent(String role) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message.getRole().equals(role)) {
                return message.getContent();
            }
        }
        return null;
    }
}
