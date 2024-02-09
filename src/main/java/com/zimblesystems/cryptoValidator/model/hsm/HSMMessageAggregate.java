package com.zimblesystems.cryptoValidator.model.hsm;


import in.nmaloth.payments.keys.constants.HSMCommand;
import in.nmaloth.payments.keys.constants.KeyType;

public class HSMMessageAggregate {

    private String generatedId;
    private HSMCommand hsmCommand;
    private KeyType keyType;
    private String messageId;
    private HSMResult hsmResult;
    private boolean complete;
    private int numberOfTries;
    private int totalTries;

    public String getGeneratedId() {
        return generatedId;
    }

    public void setGeneratedId(String generatedId) {
        this.generatedId = generatedId;
    }

    public HSMCommand getHsmCommand() {
        return hsmCommand;
    }

    public void setHsmCommand(HSMCommand hsmCommand) {
        this.hsmCommand = hsmCommand;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(KeyType keyType) {
        this.keyType = keyType;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public HSMResult getHsmResult() {
        return hsmResult;
    }

    public void setHsmResult(HSMResult hsmResult) {
        this.hsmResult = hsmResult;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public int getNumberOfTries() {
        return numberOfTries;
    }

    public void setNumberOfTries(int numberOfTries) {
        this.numberOfTries = numberOfTries;
    }

    public int getTotalTries() {
        return totalTries;
    }

    public void setTotalTries(int totalTries) {
        this.totalTries = totalTries;
    }
}
