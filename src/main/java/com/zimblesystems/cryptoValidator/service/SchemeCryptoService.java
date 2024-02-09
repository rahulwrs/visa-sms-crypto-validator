package com.zimblesystems.cryptoValidator.service;


import com.zimblesystems.cryptoValidator.model.ChipData;

public interface SchemeCryptoService {

    String extractServiceCodesFromCavv(byte[] bytesCavv);
    String extractExpiryDateFromCavv(byte[] bytesCavv);
    String extractCavv(byte[] bytesCavv);
    boolean hasBeenApproved(byte[] cavv);
    String identifyDki(String  iad,String iadFormat);
    String identifyCvr(String iad,String iadFormat);
    String identifyCvn(String iad,String iadFormat);
    boolean validateIad(String iad, String iadFormat);
    String buildTransactionData(ChipData chipData, String iadFormat, String pan);
    String getThalesSchemeId();


}
