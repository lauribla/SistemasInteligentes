package es.uni.mas.model;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private String sender;
    private String text;
    private long timestamp;

    public ChatMessage(String sender, String text) {
        this.sender = sender;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + sender + "]: " + text;
    }
}
