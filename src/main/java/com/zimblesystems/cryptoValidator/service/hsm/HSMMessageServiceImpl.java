package com.zimblesystems.cryptoValidator.service.hsm;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;
import com.zimblesystems.cryptoValidator.exception.HSMKeyNotFoundException;
import com.zimblesystems.cryptoValidator.model.hsm.*;
import com.zimblesystems.cryptoValidator.service.CacheService;
import com.zimblesystems.cryptoValidator.service.VaultService;
import com.zimblesystems.cryptoValidator.service.tcp.TCPClientService;
import in.nmaloth.payments.constants.EntryMode;
import in.nmaloth.payments.keys.HSMKey;
import in.nmaloth.payments.keys.constants.HSMCommand;
import in.nmaloth.payments.keys.constants.KeyType;
import in.nmaloth.payments.keys.constants.PinBlockFormat;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class HSMMessageServiceImpl implements HSMMessageService {

    private AtomicInteger generatedId = new AtomicInteger(0);
    private AtomicInteger countDown = new AtomicInteger(0);

    private final TCPClientService tcpServices;
    private final HSMInterface hsmInterface;
    private final CacheService cacheService;
    private final VaultService vaultService;


    public HSMMessageServiceImpl(
            TCPClientService tcpServices,
            HSMInterface hsmInterface, CacheService cacheService, VaultService vaultService) {

        this.hsmInterface = hsmInterface;
        this.tcpServices = tcpServices;
        this.cacheService = cacheService;
        this.vaultService = vaultService;
    }

    @Override
    public void createMessageCommon(String messageId, String instance,
                                    String messageTypeId, EntryMode entryMode) {


        MessageCommon messageCommon = new MessageCommon();

        messageCommon.setMessageSendForAggregation(false);
        messageCommon.setInstance(instance);
        messageCommon.setMessageId(messageId);
        messageCommon.setMessageTypeId(messageTypeId);
        messageCommon.setEntryMode(entryMode);
        messageCommon.setStatusList(new ArrayList<>());
        messageCommon.setHsmMessageAggregateList(new ArrayList<>());

        cacheService.addToCache(messageCommon);

    }


    @Override
    public byte[] validateCVC(CVCModel cvcModel, String messageId) throws HSMKeyNotFoundException {

        return validateCVC(cvcModel,messageId,KeyType.CVK,HSMCommand.CVC_VAL,"00",1);
    }

    @Override
    public byte[] validateCVC2(CVCModel cvcModel, String messageId) throws HSMKeyNotFoundException {
        return validateCVC(cvcModel,messageId,KeyType.CVK2,HSMCommand.CVC2_VAL,"00",1);
    }

    @Override
    public byte[] validateCVC_3D(CVCModel cvcModel, String messageId) throws HSMKeyNotFoundException {
        return validateCVC(cvcModel,messageId,KeyType.CVK_3D,HSMCommand.CAVV_VAL,"00",1);
    }


    @Override
    public List<byte[]> validateCVCD(DynamicCvcModel dynamicCvcModel, String messageId) throws HSMKeyNotFoundException {

        String generatedId = houseKeepingHeader(messageId, KeyType.CVK_D, HSMCommand.DYNAMIC_CVC_VAL,dynamicCvcModel.getNumberOfTries());
        List<byte[]> byteList = new ArrayList<>();
        for (CVCModel cvcModel: dynamicCvcModel.getCvcModelList()) {
            HSMKey hsmKey = getHsmKey(cvcModel.getOrg(),cvcModel.getProduct(), KeyType.CVK_D, "00");
            byteList.add(getByteMessage(hsmInterface.validateCVV(hsmKey,generatedId,cvcModel.getInstrument(),cvcModel.getExpiryDate(),cvcModel.getServiceCode(),cvcModel.getCvc())));
        }


        return byteList;
    }

    private byte[] validateCVC(CVCModel cvcModel, String messageId, KeyType keyType, HSMCommand hsmCommand, String zone,int totalTries) throws HSMKeyNotFoundException {

        String generatedId = houseKeepingHeader(messageId, keyType, hsmCommand,totalTries);
        HSMKey hsmKey = getHsmKey(cvcModel.getOrg(),cvcModel.getProduct(), keyType,zone);
        return getByteMessage(hsmInterface.validateCVV(hsmKey,generatedId,cvcModel.getInstrument(),cvcModel.getExpiryDate(),cvcModel.getServiceCode(),cvcModel.getCvc()));
    }


    @Override
    public byte[] validatePin(PinModel pinModel, String messageId,String decimalTable) throws HSMKeyNotFoundException {

        String generatedId = houseKeepingHeader(messageId, KeyType.PK, HSMCommand.PIN_VAL,1);
        HSMKey hsmKey = getHsmKey(pinModel.getOrg(),pinModel.getProduct(), KeyType.PK,"00");
        HSMKey hsmKeyZone = getHsmKey(pinModel.getOrg(),pinModel.getProduct(),KeyType.ZPK,pinModel.getZone());

        String message = hsmInterface.validatePinBlock(hsmKeyZone, hsmKey, generatedId,
                                                        getPan(pinModel.getInstrument()),
                                                        getPinOffset(pinModel.getPinOffset().toString(), pinModel.getPinLength()),
                                                        getPinLength(pinModel.getPinLength().toString()),
                                                        getPinBlkFmt(pinModel.getPinBlkFmt()),pinModel.getPin(),decimalTable);

        return getByteMessage(message);
    }

    private String getPan(String pan) {

        if(pan.length() > 16){
            pan = pan.substring(pan.length() - 16,16);
        } else if(pan.length() < 16){
            pan = new StringBuilder()
                    .append("0".repeat(16 - pan.length()))
                    .append(pan)
                    .toString();
        }
        return pan;
    }
    private String getPinOffset(String pinOffset,int pinLength) {
        pinOffset = new StringBuilder()
                .append("0".repeat(pinLength - pinOffset.length()))
                .append(pinOffset)
                .append("F".repeat(12 - pinLength))
                .toString();
        return pinOffset;
    }
    private String getPinLength(String pinLength) {
        pinLength = new StringBuilder()
                .append("0".repeat(2-pinLength.length()))
                .append(pinLength)
                .toString();
        return pinLength;
    }
    private String getPinBlkFmt(PinBlockFormat pinBlockFormat){
        switch (pinBlockFormat){
            case ISO_0:
                return "00";
            case ISO_1:
                return "01";
            case ISO_2:
                return "02";
            case ISO_3:
                return "03";
            case ISO_4:
                return "04";
            case VISA_4:
                return "V4";
            case DOCUTEL:
                return "D";
            default:
                return "DI";
        }
    }

    @Override
    public byte[] validateARQCAndGenerateARPC(ARQCModel arqcModel, String messageId) throws HSMKeyNotFoundException {

        String generatedId = houseKeepingHeader(messageId, KeyType.ARQCK, HSMCommand.ARQC_VAL,1);
        HSMKey hsmKey = getHsmKey(arqcModel.getOrg(),arqcModel.getProduct(),KeyType.ARQCK, arqcModel.getZone());

//        int txnDataLength = arqcModel.getTransactionData().length();
//        int rem = txnDataLength % 8;
//        String transactionData = arqcModel.getTransactionData();
//        if(rem > 0){
//            transactionData = new StringBuilder()
//                    .append(transactionData)
//                    .append("0".repeat(8 - rem))
//                    .toString()
//                    ;
//            txnDataLength = txnDataLength + rem;
//        }
        String  message = hsmInterface.validateARQCAndGenerateARPC(hsmKey,generatedId,arqcModel.getSchemeId(),
                getPan(arqcModel.getPan(),arqcModel.getPanSeq()),arqcModel.getAtc(),arqcModel.getUnpredictableNumber(),
                getTxnDataLength(arqcModel.getTransactionData().length()),arqcModel.getTransactionData(),arqcModel.getArqc(),
                stringToHexString(arqcModel.getArc()));

//        log.info("#########$$$$$ {}",message);
        return getByteMessage(message);
    }

    @Override
    public byte[] sendDiagnostics(String messageId) {
        String generatedId = houseKeepingHeader(messageId);

        String  message = hsmInterface.diagnostics(generatedId);
        return getByteMessage(message);


    }


//    private byte[] generateCVC(CVCDTO cvcDto, String messageId, KeyType keyType, HSMCommand hsmCommand, String zone) throws HSMKeyNotFoundException {
//
//        String generatedId = houseKeepingHeader(messageId, keyType, hsmCommand);
//        HSMKey hsmKey = getHsmKey(cvcDto.getOrg(),cvcDto.getProduct(), keyType,zone);
//        return getByteMessage(cvvService.createCVV(hsmKey,generatedId,cvcDto.getInstrument(),cvcDto.getExpiryDate(),cvcDto.getServiceCode()));
//    }

    private String getPan(String instrument, String panSequence) {

        String pan = new StringBuilder()
                .append(instrument)
                .append(panSequence)
                .toString();
        if (pan.length() > 16) {
            return pan.substring(pan.length() - 16);
        } else if (pan.length() < 16) {
            return new StringBuilder()
                    .append("0".repeat(16 - pan.length()))
                    .append(pan)
                    .toString();
        }
        return pan;

    }


    private String getTxnDataLength(int txnDataLength){

        String txnDataLengthString = Integer.toHexString(txnDataLength/2);
        txnDataLengthString = txnDataLengthString.toUpperCase();
        if(txnDataLengthString.length() == 1){
            return  "0" + txnDataLengthString;
        }
        return txnDataLengthString;

    }

    private String stringToHexString(String message){

        return BaseEncoding.base16().encode(message.getBytes());
    }

    @Override
    public void sendMessage(byte[] message) {
        tcpServices.sendMessage(message);
    }

    @Override
    public void sendMessage(List<byte[]> messageList) {

        for (byte[] message:messageList) {

            sendMessage(message);
        }
    }


    private void initializeHSMMessage(KeyType keyType, String messageId,
                                      HSMCommand command, String generatedId,int totalTries) {


        HSMMessageAggregate hsmMessageAggregate = new HSMMessageAggregate();
        hsmMessageAggregate.setGeneratedId(generatedId);
        hsmMessageAggregate.setHsmCommand(command);
        hsmMessageAggregate.setMessageId(messageId);
        hsmMessageAggregate.setComplete(false);
        hsmMessageAggregate.setNumberOfTries(0);
        hsmMessageAggregate.setTotalTries(totalTries);

        cacheService.addToCache(hsmMessageAggregate);
    }

    private void initializeHSMMessage( String messageId,
                                      HSMCommand command, String generatedId,int totalTries) {


        HSMMessageAggregate hsmMessageAggregate = new HSMMessageAggregate();
        hsmMessageAggregate.setGeneratedId(generatedId);
        hsmMessageAggregate.setHsmCommand(command);
        hsmMessageAggregate.setMessageId(messageId);
        hsmMessageAggregate.setComplete(false);
        hsmMessageAggregate.setNumberOfTries(0);
        hsmMessageAggregate.setTotalTries(totalTries);

        cacheService.addToCache(hsmMessageAggregate);
    }

    private String generateId() {

        int idNumeric = generatedId.incrementAndGet();

        if (idNumeric > 65530) {

            int checkCount = countDown.get();
            synchronized (countDown) {

                idNumeric = generatedId.get();

                if (checkCount == countDown.get() && idNumeric > 65530) {
                    countDown.incrementAndGet();
                    generatedId = new AtomicInteger(0);

                }
            }
            idNumeric = generatedId.incrementAndGet();
        }

        if (idNumeric == 30719) {
            countDown = new AtomicInteger(0);
        }
        String id = Integer.toHexString(idNumeric);

        return new StringBuilder()
                .append("0".repeat(4 - id.length()))
                .append(id)
                .toString()
                ;

    }

    private String houseKeepingHeader(String messageId, KeyType keyType, HSMCommand hsmCommand,int totalTries) {
        String generatedId = generateId();
        initializeHSMMessage(keyType, messageId, hsmCommand, generatedId,totalTries);
        MessageCommon messageCommon = cacheService.fetchCommonCache(messageId);
        messageCommon.getStatusList().add(new MessageStatus(generatedId,false));
        return generatedId;
    }


    private String houseKeepingHeader(String messageId) {
        String generatedId = generateId();
        initializeHSMMessage( messageId, HSMCommand.DIAGNOSTICS, generatedId,1);
        MessageCommon messageCommon = cacheService.fetchCommonCache(messageId);
        messageCommon.getStatusList().add(new MessageStatus(generatedId,false));
        return generatedId;
    }

    private HSMKey getHsmKey(int org,int product , KeyType keyType,String zone) throws HSMKeyNotFoundException {
        HSMKey hsmKey = vaultService.getKey(org,product,keyType,zone);

        if(hsmKey == null){

            String message = new StringBuilder()
                    .append(" No Key Found for Org : ")
                    .append(org)
                    .append(" product  : ")
                    .append(product)
                    .append(" For Key Type : ")
                    .append(keyType.toString())
                    .toString();


            throw new HSMKeyNotFoundException(message);
        }
        return hsmKey;
    }

    private byte[] getByteMessage(String message){
        byte[] length = integerToByte(message.length(), 2);
        byte[] messageBytes = message.getBytes();

        return Bytes.concat(length, messageBytes);

    }

    private byte[] getByteMessage(byte[] messageBytes){
        byte[] length = integerToByte(messageBytes.length, 2);

        return Bytes.concat(length, messageBytes);

    }

    private byte[] integerToByte(int number, int byteArrayLength) {
        byte[] bytes = new byte[byteArrayLength];
        if (byteArrayLength == 1) {
            if (number < 256) {
                bytes[0] = (byte) number;
                return bytes;
            } else {
                throw new IllegalArgumentException("Single Byte integer cannot have Length More than 255");
            }
        } else if (byteArrayLength == 2) {
            bytes[1] = (byte) number;
            bytes[0] = (byte) (number >> 8);
            return bytes;
        } else if (byteArrayLength == 3) {
            bytes[2] = (byte) number;
            bytes[1] = (byte) (number >> 8);
            bytes[0] = (byte) (number >> 16);
            return bytes;
        } else {
            throw new IllegalArgumentException("Byte Array Length greater than 3 not supported for Integer");
        }
    }


}
