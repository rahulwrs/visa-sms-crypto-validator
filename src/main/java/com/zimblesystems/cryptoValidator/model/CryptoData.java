package com.zimblesystems.cryptoValidator.model;


import in.nmaloth.payments.constants.EntryMode;
import in.nmaloth.payments.constants.RecurringTrans;
import in.nmaloth.payments.constants.network.NetworkType;
import in.nmaloth.payments.keys.constants.PinBlockFormat;

import java.time.LocalDate;

public class CryptoData {

    private String messageId;
    private String messageTypeId;
    private String aggregatorInstance;
    private String instrument;
    private String pinBlock;
    private Integer zoneKeyIndex;
    private PinBlockFormat pinBlockFormat;
    private String cavvResult;
    private String onlineCamResult;
    private String cvv_ICvvResult;
    private String cvv2Result;
    private String cvv;
    private LocalDate expiryDate;
    private String serviceCode;
    private String cvv2;
    private byte[] cavv;
    private RecurringTrans recurringTrans;
//    private ChipInfo chipInfo;
    private EntryMode entryMode;
    private ChipData chipData;
    private int cryptoOrg;
    private int cryptoProduct;
    private String zone;
    private String chipVersion;
    private String iadFormat;
    private Integer chipSeq;
    private Integer atc;
    private String schemeId;
    private Boolean dynamicCVV;
    private Integer pinOffset;
    private Integer pinLength;
    private NetworkType networkType;


    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageTypeId() {
        return messageTypeId;
    }

    public void setMessageTypeId(String messageTypeId) {
        this.messageTypeId = messageTypeId;
    }

    public String getAggregatorInstance() {
        return aggregatorInstance;
    }

    public void setAggregatorInstance(String aggregatorInstance) {
        this.aggregatorInstance = aggregatorInstance;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getPinBlock() {
        return pinBlock;
    }

    public void setPinBlock(String pinBlock) {
        this.pinBlock = pinBlock;
    }

    public Integer getZoneKeyIndex() {
        return zoneKeyIndex;
    }

    public void setZoneKeyIndex(Integer zoneKeyIndex) {
        this.zoneKeyIndex = zoneKeyIndex;
    }

    public PinBlockFormat getPinBlockFormat() {
        return pinBlockFormat;
    }

    public void setPinBlockFormat(PinBlockFormat pinBlockFormat) {
        this.pinBlockFormat = pinBlockFormat;
    }

    public String getCavvResult() {
        return cavvResult;
    }

    public void setCavvResult(String cavvResult) {
        this.cavvResult = cavvResult;
    }

    public String getOnlineCamResult() {
        return onlineCamResult;
    }

    public void setOnlineCamResult(String onlineCamResult) {
        this.onlineCamResult = onlineCamResult;
    }

    public String getCvv_ICvvResult() {
        return cvv_ICvvResult;
    }

    public void setCvv_ICvvResult(String cvv_ICvvResult) {
        this.cvv_ICvvResult = cvv_ICvvResult;
    }

    public String getCvv2Result() {
        return cvv2Result;
    }

    public void setCvv2Result(String cvv2Result) {
        this.cvv2Result = cvv2Result;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getCvv2() {
        return cvv2;
    }

    public void setCvv2(String cvv2) {
        this.cvv2 = cvv2;
    }

    public byte[] getCavv() {
        return cavv;
    }

    public void setCavv(byte[] cavv) {
        this.cavv = cavv;
    }

    public RecurringTrans getRecurringTrans() {
        return recurringTrans;
    }

    public void setRecurringTrans(RecurringTrans recurringTrans) {
        this.recurringTrans = recurringTrans;
    }

//    public ChipInfo getChipInfo() {
//        return chipInfo;
//    }
//
//    public void setChipInfo(ChipInfo chipInfo) {
//        this.chipInfo = chipInfo;
//    }

    public EntryMode getEntryMode() {
        return entryMode;
    }

    public void setEntryMode(EntryMode entryMode) {
        this.entryMode = entryMode;
    }

    public ChipData getChipData() {
        return chipData;
    }

    public void setChipData(ChipData chipData) {
        this.chipData = chipData;
    }

    public int getCryptoOrg() {
        return cryptoOrg;
    }

    public void setCryptoOrg(int cryptoOrg) {
        this.cryptoOrg = cryptoOrg;
    }

    public int getCryptoProduct() {
        return cryptoProduct;
    }

    public void setCryptoProduct(int cryptoProduct) {
        this.cryptoProduct = cryptoProduct;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getChipVersion() {
        return chipVersion;
    }

    public void setChipVersion(String chipVersion) {
        this.chipVersion = chipVersion;
    }

    public String getIadFormat() {
        return iadFormat;
    }

    public void setIadFormat(String iadFormat) {
        this.iadFormat = iadFormat;
    }

    public Integer getChipSeq() {
        return chipSeq;
    }

    public void setChipSeq(Integer chipSeq) {
        this.chipSeq = chipSeq;
    }

    public Integer getAtc() {
        return atc;
    }

    public void setAtc(Integer atc) {
        this.atc = atc;
    }

    public String getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(String schemeId) {
        this.schemeId = schemeId;
    }

    public Boolean getDynamicCVV() {
        return dynamicCVV;
    }

    public void setDynamicCVV(Boolean dynamicCVV) {
        this.dynamicCVV = dynamicCVV;
    }

    public Integer getPinOffset() {
        return pinOffset;
    }

    public void setPinOffset(Integer pinOffset) {
        this.pinOffset = pinOffset;
    }

    public Integer getPinLength() {
        return pinLength;
    }

    public void setPinLength(Integer pinLength) {
        this.pinLength = pinLength;
    }

    public NetworkType getNetworkType() {
        return networkType;
    }

    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
    }
}
