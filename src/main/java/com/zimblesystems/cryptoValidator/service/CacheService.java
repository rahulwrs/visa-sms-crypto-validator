package com.zimblesystems.cryptoValidator.service;

import com.zimblesystems.cryptoValidator.model.hsm.HSMMessageAggregate;
import com.zimblesystems.cryptoValidator.model.hsm.HSMResult;
import com.zimblesystems.cryptoValidator.model.hsm.MessageCommon;

public interface CacheService {

    void removeAll(MessageCommon messageCommon);
    void removeCommonCache(String messageId);

    void validateHSMResults(HSMResult hsmResult);

    void addToCache(MessageCommon messageCommon);
    void addToCache(HSMMessageAggregate hsmMessageAggregate);

    HSMMessageAggregate fetchAggregateCache(String generatedId);

    MessageCommon fetchCommonCache(String messageId);
}
