package com.zimblesystems.cryptoValidator.model.hsm;


public class MessageStatus {

    private String messageId;
    private boolean completed;

    public MessageStatus(String messageId, boolean completed) {
        this.messageId = messageId;
        this.completed = completed;
    }

    public MessageStatus() {
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
