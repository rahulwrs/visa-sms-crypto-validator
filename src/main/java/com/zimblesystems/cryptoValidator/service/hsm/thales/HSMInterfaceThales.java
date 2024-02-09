package com.zimblesystems.cryptoValidator.service.hsm.thales;

import com.google.common.io.BaseEncoding;
import com.zimblesystems.cryptoValidator.service.hsm.HSMInterface;
import in.nmaloth.payments.keys.HSMKey;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HSMInterfaceThales implements HSMInterface {

//    private final String decimalTable = "0123456789012345";



    @Override
    public String validateARQCAndGenerateARPC(HSMKey hsmKey, String header, String schemeId,
                                              String pan, String atc, String unpredictableNumber,
                                              String transactionDataLength, String transactionData,
                                              String arqc, String arc) {



        return new  StringBuilder()
                .append(header)
                .append("KQ")
                .append("1")
                .append(schemeId)
                .append(hsmKey.getKeyScheme())
                .append(hsmKey.getKey())
                .append(pan)
                .append(atc)
                .append(unpredictableNumber)
                .append(transactionDataLength)
                .append(transactionData)
                .append(";")
                .append(arqc)
                .append(arc)
                .toString()
                ;

    }

    @Override
    public String diagnostics(String generatedId) {
        return new StringBuilder()
                .append(generatedId)
                .append("NC")
                .toString()
                ;
    }

//    @Override
//    public byte[] validateARQCAndGenerateARPC(HSMKey hsmKey, String header, String schemeId, String pan, String atc, String unpredictableNumber, String transactionDataLength, String transactionData, String arqc, String arc) {
//
//
//        String messageInitial = new StringBuilder()
//                .append(header)
//                .append("KQ")
//                .append("1")
//                .append(schemeId)
//                .append(hsmKey.getKeyScheme())
//                .append(hsmKey.getKeyA())
//                .append(hsmKey.getKeyB())
//                .append(pan)
//                .append(atc)
//                .append(unpredictableNumber)
//                .append(transactionDataLength)
//                .append(transactionData)
//                .append(";")
//
//                .toString();
//
//        byte[] panBytes = stringToHex(pan,16);
//        byte[] atcByte = stringToHex(atc,4);
//        byte[] un = stringToHex(unpredictableNumber,8);
//        byte[] transactionDataByte = stringToHex(transactionData,transactionDataLength.length());
//        byte[] transactionLengthByte = transactionDataLength.getBytes();
//        byte[] delimiter = ";".getBytes();
//        byte[] arqcByte = stringToHex(arqc,16);
//        byte[] arcByte = stringToHex(arc,4);
//
//
//        return Bytes.concat(messageInitial.getBytes(),delimiter,arqcByte,arcByte);
//    }

    @Override
    public String validateCVV(HSMKey hsmKey, String header, String instrument, String expDate, String serviceCode, String cvv) {

        return new StringBuilder().append(header)
                .append("CY")
                .append(hsmKey.getKeyScheme())
                .append(hsmKey.getKey())
                .append(cvv)
                .append(instrument)
                .append(";")
                .append(expDate)
                .append(serviceCode)
                .toString();

    }

    public static byte[] stringToHex(String input, int length) {
        String stringValue = input;
        if ((length % 2) == 1) {
            length = length + 1;
        }

        if (stringValue.length() < length) {


            String padString = "0".repeat(length - stringValue.length());
            StringBuilder sb = new StringBuilder();
            sb.append(padString).append(stringValue);
            stringValue = sb.toString();

        }
        return BaseEncoding.base16().decode(stringValue);

    }



    @Override
    public String validatePinBlock(HSMKey hsmKeyZone, HSMKey hsmKeyPin, String header, String instrument,
                                   String pinOffSet, String pinLength, String pinBlkFormat, String pinBlock, String decimalizationTable) {

        String pan12 = instrument.substring(3,15);

        StringBuilder stringBuilder = new StringBuilder();
        return stringBuilder.append(header)
                .append("EA")
                .append(hsmKeyZone.getKeyScheme())
                .append(hsmKeyZone.getKey())
                .append(hsmKeyPin.getKeyScheme())
                .append(hsmKeyPin.getKey())
                .append("12")
                .append(pinBlock)
                .append(pinBlkFormat)
                .append(pinLength)
                .append(pan12)
                .append(decimalizationTable)
                .append(createUserData(instrument))
                .append(pinOffSet)
                .toString();


    }


    private String createUserData(String pan){

        String pan10 = pan.substring(0,10);
        String checkDigit = pan.substring(15);
        return new StringBuilder()
                .append(pan10)
                .append("N")
                .append(checkDigit)
                .toString();
    }
}
