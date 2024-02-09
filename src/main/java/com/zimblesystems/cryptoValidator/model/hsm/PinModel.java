package com.zimblesystems.cryptoValidator.model.hsm;


import in.nmaloth.payments.keys.constants.PinBlockFormat;

public class PinModel {

    private int org;
    private int product;
    private String instrument;
    private Integer pinOffset;
    private Integer pinLength;
    private String pin;
    private String zone;
    private PinBlockFormat pinBlkFmt;

    public int getOrg() {
        return org;
    }

    public void setOrg(int org) {
        this.org = org;
    }

    public int getProduct() {
        return product;
    }

    public void setProduct(int product) {
        this.product = product;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
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

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public PinBlockFormat getPinBlkFmt() {
        return pinBlkFmt;
    }

    public void setPinBlkFmt(PinBlockFormat pinBlkFmt) {
        this.pinBlkFmt = pinBlkFmt;
    }
}
