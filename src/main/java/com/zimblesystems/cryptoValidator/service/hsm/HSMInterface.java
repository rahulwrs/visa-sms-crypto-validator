package com.zimblesystems.cryptoValidator.service.hsm;

import in.nmaloth.payments.keys.HSMKey;

public interface HSMInterface {

    String validatePinBlock(HSMKey hsmKeyZone, HSMKey hsmKeyPin, String header, String  instrument,
                            String pinOffSet, String pinLength, String pinBlkFormat, String pinBlock, String decimalizationTable);

    String validateCVV(HSMKey hsmKey,String header,String instrument,String expDate,String serviceCode,String cvv);


    String validateARQCAndGenerateARPC(HSMKey hsmKey,String header,String schemeId,
                                       String pan,String atc,String unpredictableNumber,
                                       String transactionDataLength,String transactionData,String arqc,
                                       String arc);

    String diagnostics(String generatedId);


}
