package com.zimblesystems.cryptoValidator.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.zimblesystems.cryptoValidator.model.hsm.HSMMessageAggregate;
import com.zimblesystems.cryptoValidator.model.hsm.HSMResult;
import com.zimblesystems.cryptoValidator.model.hsm.MessageCommon;
import com.zimblesystems.cryptoValidator.model.hsm.MessageStatus;
import in.nmaloth.payments.keys.constants.HSMCommand;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CacheServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheServiceImpl.class);

    private final AggregationService aggregationService;


    private final Cache<String, HSMMessageAggregate> hsmMessageAggregateCache = Caffeine.newBuilder()
            .evictionListener(this::evictionListenerAggregate)
            .expireAfterWrite(Duration.ofMillis(1500))
            .build();



    private final Cache<String, MessageCommon> commonCache = Caffeine.newBuilder()
            .evictionListener(this::evictionListenerCommon)
            .expireAfterWrite(Duration.ofMillis(1500))
            .build();


    public CacheServiceImpl(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }


    @Override
    public void removeAll(MessageCommon messageCommon) {

        for (MessageStatus messageStatus : messageCommon.getStatusList()) {
            hsmMessageAggregateCache.invalidate(messageStatus.getMessageId());
        }

        commonCache.invalidate(messageCommon.getMessageId());

    }

    @Override
    public void removeCommonCache(String messageId) {
        commonCache.invalidate(messageId);
    }


    @Override
    public void addToCache(MessageCommon messageCommon) {

        commonCache.put(messageCommon.getMessageId(),messageCommon);
    }

    @Override
    public void addToCache(HSMMessageAggregate hsmMessageAggregate) {

        hsmMessageAggregateCache.put(hsmMessageAggregate.getGeneratedId(),hsmMessageAggregate);
    }


    @Override
    public MessageCommon fetchCommonCache(String messageId) {
        return commonCache.getIfPresent(messageId);
    }

    @Override
    public HSMMessageAggregate fetchAggregateCache(String generatedId) {
        return hsmMessageAggregateCache.getIfPresent(generatedId);
    }


    @Override
    public void validateHSMResults(HSMResult hsmResult) {

        HSMMessageAggregate hsmMessageAggregate = hsmMessageAggregateCache.getIfPresent(hsmResult.getId());
        if(hsmMessageAggregate != null){
            hsmMessageAggregate.setNumberOfTries(hsmMessageAggregate.getNumberOfTries() + 1);
            if(hsmMessageAggregate.getTotalTries() == hsmMessageAggregate.getNumberOfTries()){
                if(!hsmMessageAggregate.isComplete()){
                    hsmMessageAggregate.setComplete(true);
                    hsmMessageAggregate.setHsmResult(hsmResult);

                    aggregationService.sendHSMResponseToProcessor(hsmMessageAggregate);
//                    updateMessageCommon(hsmMessageAggregate);;

                }
            } else {
                if(hsmMessageAggregate.getHsmCommand().equals(HSMCommand.DYNAMIC_CVC_VAL)){
                    if(hsmResult.getResponseCode().equals("00")){

                        hsmMessageAggregate.setComplete(true);
                        hsmMessageAggregate.setHsmResult(hsmResult);
                        aggregationService.sendHSMResponseToProcessor(hsmMessageAggregate);

//                        updateMessageCommon(hsmMessageAggregate);
                    }
                }
            }
        }

    }



    private boolean checkForComplete(List<MessageStatus> messageStatuses){

        for(MessageStatus messageStatus : messageStatuses){
            if(!messageStatus.isCompleted()){
                return false;
            }
        }
        return true;
    }



    private void updateMessageCommon(HSMMessageAggregate hsmMessageAggregate){

        MessageCommon messageCommon = commonCache.getIfPresent(hsmMessageAggregate.getMessageId());

        if(messageCommon != null){
            messageCommon.getHsmMessageAggregateList().add(hsmMessageAggregate);
            messageCommon.getStatusList().stream().filter(messageStatus -> messageStatus.getMessageId().equals(hsmMessageAggregate.getGeneratedId()))
                    .findFirst().ifPresent(messageStatus -> messageStatus.setCompleted(true));

            if(checkForComplete(messageCommon.getStatusList())){
                messageCommon.getCompletableFuture().complete(messageCommon.getHsmMessageAggregateList().stream()
                        .map(hsmMessageAggregate1 -> hsmMessageAggregate1.getHsmResult())
                        .collect(Collectors.toList()));

                commonCache.invalidate(messageCommon.getMessageId());
                messageCommon.getHsmMessageAggregateList()
                        .forEach(hsmMessageAggregate1 -> hsmMessageAggregateCache.invalidate(hsmMessageAggregate1.getGeneratedId()));
            }

        }

    }

    private void evictionListenerAggregate(Object o, Object o2, RemovalCause removalCause) {

        if(removalCause.wasEvicted()){

            HSMMessageAggregate hsmMessageAggregate = (HSMMessageAggregate) o2;
            logger.error(" Aggregation HSM not completed for id: {} for {}", hsmMessageAggregate.getMessageId(),removalCause.toString());
        }


    }

    private void evictionListenerCommon(Object o, Object o2, RemovalCause removalCause) {

        if(removalCause.wasEvicted()){
            MessageCommon messageCommon = (MessageCommon) o2;
            logger.error(" Aggregation Common not completed for id: {} for {}", messageCommon.getMessageId(),removalCause.toString());
        }


    }
}
