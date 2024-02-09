package com.zimblesystems.cryptoValidator.model.hsm;


import java.util.List;

public class DynamicCvcModel {

    private int numberOfTries;
    private List<CVCModel> cvcModelList;

    public int getNumberOfTries() {
        return numberOfTries;
    }

    public void setNumberOfTries(int numberOfTries) {
        this.numberOfTries = numberOfTries;
    }

    public List<CVCModel> getCvcModelList() {
        return cvcModelList;
    }

    public void setCvcModelList(List<CVCModel> cvcModelList) {
        this.cvcModelList = cvcModelList;
    }
}
