package com.zimblesystems.cryptoValidator.model.hsm;


import in.nmaloth.payments.keys.constants.CVCType;

public class CVCModel {

    private int org;
    private int product;
    private String instrument;
    private String expiryDate;
    private String serviceCode;
    private String cvc;
    private CVCType cvcType;
    private String zone;

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

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(String cvc) {
        this.cvc = cvc;
    }

    public CVCType getCvcType() {
        return cvcType;
    }

    public void setCvcType(CVCType cvcType) {
        this.cvcType = cvcType;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
