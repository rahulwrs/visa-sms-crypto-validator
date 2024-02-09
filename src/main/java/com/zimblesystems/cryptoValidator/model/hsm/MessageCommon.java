package com.zimblesystems.cryptoValidator.model.hsm;

import in.nmaloth.payments.constants.EntryMode;
import in.nmaloth.payments.keys.constants.CryptoSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public class MessageCommon {

    private String messageId;
    private String instance;
    private String messageTypeId;
    private CryptoSource source;
    private EntryMode entryMode;
    private boolean messageSendForAggregation;
    private List<MessageStatus> statusList;
    private List<HSMMessageAggregate> hsmMessageAggregateList;
    private CompletableFuture<List<HSMResult>> completableFuture;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getMessageTypeId() {
        return messageTypeId;
    }

    public void setMessageTypeId(String messageTypeId) {
        this.messageTypeId = messageTypeId;
    }

    public CryptoSource getSource() {
        return source;
    }

    public void setSource(CryptoSource source) {
        this.source = source;
    }

    public EntryMode getEntryMode() {
        return entryMode;
    }

    public void setEntryMode(EntryMode entryMode) {
        this.entryMode = entryMode;
    }

    public boolean isMessageSendForAggregation() {
        return messageSendForAggregation;
    }

    public void setMessageSendForAggregation(boolean messageSendForAggregation) {
        this.messageSendForAggregation = messageSendForAggregation;
    }

    public List<MessageStatus> getStatusList() {
        return statusList;
    }

    public void setStatusList(List<MessageStatus> statusList) {
        this.statusList = statusList;
    }

    public List<HSMMessageAggregate> getHsmMessageAggregateList() {
        return hsmMessageAggregateList;
    }

    public void setHsmMessageAggregateList(List<HSMMessageAggregate> hsmMessageAggregateList) {
        this.hsmMessageAggregateList = hsmMessageAggregateList;
    }

    public CompletableFuture<List<HSMResult>> getCompletableFuture() {
        return completableFuture;
    }

    public void setCompletableFuture(CompletableFuture<List<HSMResult>> completableFuture) {
        this.completableFuture = completableFuture;
    }
}
