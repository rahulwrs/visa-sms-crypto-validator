package com.zimblesystems.cryptoValidator.service;

import com.zimblesystems.cryptoValidator.model.hsm.HSMMessageAggregate;
import com.zimblesystems.cryptoValidator.model.hsm.MessageCommon;
import com.zimblesystems.cryptoValidator.model.proto.aggregator.ValidationResponse;
import com.zimblesystems.cryptoValidator.model.proto.aggregator.ValidationResponseSummary;
import in.nmaloth.payments.constants.EntryMode;

public interface AggregationService {

    void sendHSMResponseToProcessor(HSMMessageAggregate hsmMessageAggregate);
    boolean checkIfAllValidationCompleted(MessageCommon messageCommon);

    ValidationResponse createCVCResponse(HSMMessageAggregate hsmMessageAggregate);
    ValidationResponse createCVC2Response(HSMMessageAggregate hsmMessageAggregate);
    ValidationResponse createCAVVResponse(HSMMessageAggregate hsmMessageAggregate);
    ValidationResponse createARQCResponse(HSMMessageAggregate hsmMessageAggregate, EntryMode entryMode);
    ValidationResponse createPinResponse(HSMMessageAggregate hsmMessageAggregate);

    ValidationResponse createValidationResponse(HSMMessageAggregate hsmMessageAggregate, EntryMode entryMode);

    ValidationResponseSummary createFinalResponseMessage(MessageCommon messageCommon);

    boolean sendMessageToAggregator(MessageCommon messageCommon);

    MessageCommon processHSMResponse(HSMMessageAggregate hsmMessageAggregate);

    void removeAllCache(MessageCommon messageCommon);
}
