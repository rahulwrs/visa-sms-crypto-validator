package com.zimblesystems.cryptoValidator.service;


import com.google.common.io.BaseEncoding;
import com.zimblesystems.cryptoValidator.model.ChipData;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VisaCryptoServiceImpl implements SchemeCryptoService{

    @Override
    public String extractServiceCodesFromCavv(byte[] bytesCavv) {

        byte[] bytes = new byte[2];
        System.arraycopy(bytesCavv,0,bytes,0,2);
        return hexToString(bytes).substring(1);
    }


    @Override
    public String extractExpiryDateFromCavv(byte[] bytesCavv) {

        byte[] bytes = new byte[2];
        System.arraycopy(bytesCavv,13,bytes,0,2);
        return hexToString(bytes);
    }


    @Override
    public String extractCavv(byte[] bytesCavv) {

        byte[] bytes = new byte[2];
        System.arraycopy(bytesCavv,3,bytes,0,2);

        return hexToString(bytes).substring(1);
    }


    @Override
    public boolean hasBeenApproved(byte[] cavv) {

        byte[] bytes = new byte[1];
        bytes[0] = cavv[0];
        String authenticationResult = hexToString(bytes).substring(1);

        if(authenticationResult.equals("9")) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String identifyDki(String iad, String iadFormat) {

        if(iadFormat.equals("2")){
            return populateDkiIADFormat2(iad);
        }
        return populateDKI(iad);
    }

    private String populateDKI(String iad) {
        return iad.substring(2,4);
    }

    private String populateDkiIADFormat2(String iad) {
        return iad.substring(4,6);
    }

    @Override
    public String identifyCvr(String iad, String iadFormat) {
        if(iadFormat.equals("2")){
            return populateCvrIAD2(iad);
        }
        return populateCvrIAD(iad);
    }

    private String populateCvrIAD(String iad) {

        return iad.substring(6,14);
    }

    private String populateCvrIAD2(String iad) {
        return iad.substring(6,16);
    }

    @Override
    public String identifyCvn(String iad, String iadFormat) {

        if(iadFormat.equals("2")) {
            return populateCvnIad2(iad);
        }
        return populateCvn(iad);
    }

    @Override
    public boolean validateIad(String iad, String iadFormat) {

        if(iadFormat.equals("2")) {
            return validateIadFormat2(iad);
        }
        return validateIad(iad);
    }

    @Override
    public String buildTransactionData(ChipData chipData, String iadFormat, String pan) {

        switch (identifyCvn(chipData.getIad(),iadFormat)) {
            case "12": {
                return buildTransactionDataCVN18(chipData,pan);
            }
            default: {
                return buildTransactionDataCVN10(chipData,pan);
            }
        }
    }

    @Override
    public String getThalesSchemeId() {
        return "0";
    }

    private String buildTransactionDataCVN10(ChipData chipData,String pan) {

        return new StringBuilder()
                .append(chipData.getTransactionAmount())
                .append(chipData.getOtherAmount())
                .append(chipData.getTerminalCountryCode())
                .append(chipData.getTvr())
                .append(chipData.getTransactionCurrency())
                .append(chipData.getTransactionDate())
                .append(chipData.getTransactionType())
                .append(chipData.getUnpredictableNumber())
                .append(chipData.getAip())
                .append(chipData.getAtc())
                .append(identifyCvr(chipData.getIad(),"1"))
                .toString();

    }

    private String buildTransactionDataCVN18(ChipData chipData,String pan) {


        return new StringBuilder()
                .append(chipData.getTransactionAmount())
                .append(chipData.getOtherAmount())
                .append(chipData.getTerminalCountryCode())
                .append(chipData.getTvr())
                .append(chipData.getTransactionCurrency())
                .append(chipData.getTransactionDate())
                .append(chipData.getTransactionType())
                .append(chipData.getUnpredictableNumber())
                .append(chipData.getAip())
                .append(chipData.getAtc())
                .append(chipData.getIad())
                .toString();
    }


    private boolean validateIadFormat2(String iad) {
        if(iad.length() < 16) {
            return false;
        }
        return true;
    }

    private boolean validateIad(String iad) {
        if(iad.length() < 14) {
            return false;
        }
        return true;
    }

    private String populateCvn(String iad) {
        return iad.substring(4,6);
    }

    private String populateCvnIad2(String iad) {
        return iad.substring(2,4);
    }

    public static String hexToString(byte[] input) {
        return BaseEncoding.base16().encode(input);

    }


}
