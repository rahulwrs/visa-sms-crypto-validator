package com.zimblesystems.cryptoValidator.service.hsm;


import com.zimblesystems.cryptoValidator.exception.HSMKeyNotFoundException;
import com.zimblesystems.cryptoValidator.model.hsm.ARQCModel;
import com.zimblesystems.cryptoValidator.model.hsm.CVCModel;
import com.zimblesystems.cryptoValidator.model.hsm.DynamicCvcModel;
import com.zimblesystems.cryptoValidator.model.hsm.PinModel;
import in.nmaloth.payments.constants.EntryMode;

import java.util.List;

public interface HSMMessageService {

    void createMessageCommon(String messageId, String instance, String messageTypeId, EntryMode entryMode);


    byte[] validateCVC(CVCModel cvcModel, String messageId) throws HSMKeyNotFoundException;
    byte[] validateCVC2(CVCModel cvcModel, String messageId) throws HSMKeyNotFoundException;
    byte[] validateCVC_3D(CVCModel cvcModel, String messageId) throws HSMKeyNotFoundException;
    List<byte[]> validateCVCD(DynamicCvcModel dynamicCvcModel, String messageId) throws HSMKeyNotFoundException;
    byte[] validatePin(PinModel pinModel, String messageId,String decimalTable)throws HSMKeyNotFoundException;

    byte[] validateARQCAndGenerateARPC(ARQCModel arqcModel, String messageId) throws HSMKeyNotFoundException;

    byte[] sendDiagnostics(String messageId);



    void sendMessage(byte[] message);
    void sendMessage(List<byte[]> messageList);
}
