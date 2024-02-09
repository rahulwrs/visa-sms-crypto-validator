package com.zimblesystems.cryptoValidator.service;

import com.zimblesystems.cryptoValidator.config.EventProcessors;
import com.zimblesystems.cryptoValidator.config.ServiceNames;
import com.zimblesystems.cryptoValidator.listeners.MessageListener;
import com.zimblesystems.cryptoValidator.listeners.MessageListenerImpl;
import com.zimblesystems.cryptoValidator.model.hsm.HSMMessageAggregate;
import com.zimblesystems.cryptoValidator.model.hsm.MessageCommon;
import com.zimblesystems.cryptoValidator.model.hsm.MessageStatus;
import com.zimblesystems.cryptoValidator.model.proto.aggregator.ValidationResponse;
import com.zimblesystems.cryptoValidator.model.proto.aggregator.ValidationResponseSummary;
import com.zimblesystems.cryptoValidator.processors.EventIncomingProcessor;
import com.zimblesystems.cryptoValidator.processors.EventIncomingProcessorImpl;
import in.nmaloth.payments.constants.EntryMode;
import in.nmaloth.payments.constants.ResponseResults.ValidationResult;
import in.nmaloth.payments.constants.ServiceResponse;
import in.nmaloth.payments.constants.ids.FieldID;
import in.nmaloth.payments.constants.ids.ServiceID;
import in.nmaloth.payments.constants.ids.ServiceNamesConstant;
import in.nmaloth.payments.keys.constants.HSMCommand;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ApplicationScoped
public class AggregationServiceImpl implements AggregationService {


    private static final Logger logger = LoggerFactory.getLogger(AggregationServiceImpl.class);
    private EventIncomingProcessor[] hsmResponseEventIncomingProcessors;

    private final CacheService cacheService;
    private final EventProcessors eventProcessors;

    @ConfigProperty(name = "hsm.response.threads")
    int hsmResponseThreads;


    public AggregationServiceImpl(CacheService cacheService, EventProcessors eventProcessors) {
        this.cacheService = cacheService;
        this.eventProcessors = eventProcessors;
    }

    public void startup(@Observes StartupEvent startupEvent) {

        if (hsmResponseThreads == 0) {
            hsmResponseThreads = 1;
        }

        hsmResponseEventIncomingProcessors = new EventIncomingProcessor[hsmResponseThreads];


        for (int i = 0; i < hsmResponseThreads; i++) {
            hsmResponseEventIncomingProcessors[i] = new EventIncomingProcessorImpl<>();
            MessageListener<HSMMessageAggregate> messageListener = new MessageListenerImpl<>();
            hsmResponseEventIncomingProcessors[i].registerFluxListeners(messageListener);

            Multi<HSMMessageAggregate> multiValidationResults = Multi.createFrom()
                    .emitter((Consumer<MultiEmitter<? super HSMMessageAggregate>>) messageListener::setEmitter);

            multiValidationResults.runSubscriptionOn(Executors.newSingleThreadExecutor())
                    .filter(hsmMessageAggregate -> !hsmMessageAggregate.getHsmCommand().equals(HSMCommand.DIAGNOSTICS))
                    .onItem().transform(hsmMessageAggregate -> processHSMResponse(hsmMessageAggregate))
                    .filter(messageCommon -> checkIfAllValidationCompleted(messageCommon))
                    .filter(messageCommon -> !sendMessageToAggregator(messageCommon))
                    .onFailure().recoverWithItem(throwable -> {

                        throwable.printStackTrace();
                        MessageCommon messageCommon = new MessageCommon();
                        messageCommon.setMessageId(ServiceNames.ERROR);
                        return messageCommon;
                    })
                    .subscribe().with(messageCommon -> {
                        logger.error(" #### implement error processing, {}", messageCommon.getMessageId());
                    })

            ;

//            validationResultsMultiArray[i] = multiValidationResults;

//            multiValidationResults
//                    .onItem().transform(validationResults -> )

        }

    }


    @Override
    public void sendHSMResponseToProcessor(HSMMessageAggregate hsmMessageAggregate) {


        if (hsmResponseThreads == 0) {
            hsmResponseThreads = 1;
        }

        int hashInt = hsmMessageAggregate.getMessageId().hashCode();
        if (hashInt < 0) {
            hashInt = hashInt * -1;
        }
        int index = hashInt % hsmResponseThreads;

        hsmResponseEventIncomingProcessors[index].processMessage(hsmMessageAggregate);

    }


    @Override
    public boolean checkIfAllValidationCompleted(MessageCommon messageCommon) {


        if (messageCommon == null) {
            return false;
        }

        logger.info("########## Entered the check");
        for (MessageStatus messageStatus : messageCommon.getStatusList()) {

            if (!messageStatus.isCompleted()) {
                return false;
            }
        }


        if (messageCommon.getCompletableFuture() != null) {
            messageCommon.getCompletableFuture()
                    .complete(messageCommon
                            .getHsmMessageAggregateList()
                            .stream()
                            .map(hsmMessageAggregate -> hsmMessageAggregate
                                    .getHsmResult()).collect(Collectors.toList()));
            removeAllCache(messageCommon);
            return false;
        }

        messageCommon.setMessageSendForAggregation(true);
        return true;

    }

    @Override
    public ValidationResponse createCVCResponse(HSMMessageAggregate hsmMessageAggregate) {

        ServiceResponse serviceResponse;
        Map<String, String> serviceFieldMap = new HashMap<>();

        if (hsmMessageAggregate.getHsmResult().getResponseCode().equals("00")) {
            serviceResponse = ServiceResponse.OK;
            serviceFieldMap.put(FieldID.CVV_RESULT.getFieldId(),
                    ValidationResult.VALIDATION_PASSED.getValidationResult());
        } else {
            serviceResponse = ServiceResponse.INVALID_CVV;
            serviceFieldMap.put(FieldID.CVV_RESULT.getFieldId(),
                    ValidationResult.VALIDATION_FAILED.getValidationResult());
        }

        return buildResponse(serviceResponse, serviceFieldMap, ServiceID.CVC_SERVICE);
    }

    @Override
    public ValidationResponse createCVC2Response(HSMMessageAggregate hsmMessageAggregate) {

        ServiceResponse serviceResponse;
        Map<String, String> serviceFieldMap = new HashMap<>();

        if (hsmMessageAggregate.getHsmResult().getResponseCode().equals("00")) {
            serviceResponse = ServiceResponse.OK;
            serviceFieldMap.put(FieldID.CVV2_RESULT.getFieldId(),
                    ValidationResult.VALIDATION_PASSED.getValidationResult());
        } else {
            serviceResponse = ServiceResponse.INVALID_CVV2;
            serviceFieldMap.put(FieldID.CVV2_RESULT.getFieldId(),
                    ValidationResult.VALIDATION_FAILED.getValidationResult());
        }

        return buildResponse(serviceResponse, serviceFieldMap, ServiceID.CVC2_SERVICE);
    }

    @Override
    public ValidationResponse createCAVVResponse(HSMMessageAggregate hsmMessageAggregate) {

        ServiceResponse serviceResponse;
        Map<String, String> serviceFieldMap = new HashMap<>();

        if (hsmMessageAggregate.getHsmResult().getResponseCode().equals("00")) {
            serviceResponse = ServiceResponse.OK;
            serviceFieldMap.put(FieldID.CAVV_RESULT.getFieldId(),
                    ValidationResult.VALIDATION_PASSED.getValidationResult());
        } else {
            serviceResponse = ServiceResponse.INVALID_CAVV;
            serviceFieldMap.put(FieldID.CAVV_RESULT.getFieldId(),
                    ValidationResult.VALIDATION_FAILED.getValidationResult());
        }


        return buildResponse(serviceResponse, serviceFieldMap, ServiceID.CAVV_SERVICE);
    }

    @Override
    public ValidationResponse createARQCResponse(HSMMessageAggregate hsmMessageAggregate, EntryMode entryMode) {

        ServiceResponse serviceResponse;
        Map<String, String> serviceFieldMap = new HashMap<>();
        serviceFieldMap.put(FieldID.ENTRY_MODE.getFieldId(), entryMode.getEntryMode());

        if (hsmMessageAggregate.getHsmResult().getResponseCode().equals("00")) {
            serviceResponse = ServiceResponse.OK;
            serviceFieldMap.put(FieldID.ARQC_RESULT.getFieldId(),
                    ValidationResult.VALIDATION_PASSED.getValidationResult());
            serviceFieldMap.put(FieldID.ARPC.getFieldId(), hsmMessageAggregate.getHsmResult().getResponseMessage());
        } else {
            serviceResponse = ServiceResponse.INVALID_ARQC;
            serviceFieldMap.put(FieldID.ARQC_RESULT.getFieldId(),
                    ValidationResult.VALIDATION_FAILED.getValidationResult());
        }

        return buildResponse(serviceResponse, serviceFieldMap, ServiceID.ARQC_SERVICE);
    }

    @Override
    public ValidationResponse createPinResponse(HSMMessageAggregate hsmMessageAggregate) {

        ServiceResponse serviceResponse;
        Map<String, String> serviceFieldMap = new HashMap<>();

        if (hsmMessageAggregate.getHsmResult().getResponseCode().equals("00")) {
            serviceResponse = ServiceResponse.OK;
        } else {
            serviceResponse = ServiceResponse.INVALID_PIN;
        }

        return buildResponse(serviceResponse, serviceFieldMap, ServiceID.PIN_SERVICE);
    }

    @Override
    public ValidationResponse createValidationResponse(HSMMessageAggregate hsmMessageAggregate, EntryMode entryMode) {

        switch (hsmMessageAggregate.getHsmCommand()) {
            case CVC_VAL: {
                return createCVCResponse(hsmMessageAggregate);
            }
            case DYNAMIC_CVC_VAL:
            case CVC2_VAL: {
                return createCVC2Response(hsmMessageAggregate);
            }
            case ARQC_VAL: {
                return createARQCResponse(hsmMessageAggregate, entryMode);
            }
            case PIN_VAL: {
                return createPinResponse(hsmMessageAggregate);
            }
            case CAVV_VAL: {
                return createCAVVResponse(hsmMessageAggregate);
            }

            default: {
                throw new RuntimeException("Invalid HSM Command Processed");
            }
        }
    }

    @Override
    public ValidationResponseSummary createFinalResponseMessage(MessageCommon messageCommon) {


        List<ValidationResponse> validationResponseList =
                new ArrayList<>();
        messageCommon.getHsmMessageAggregateList().forEach(hsmMessageAggregate -> {
            validationResponseList.add(createValidationResponse(hsmMessageAggregate, messageCommon.getEntryMode()));
        });
        return ValidationResponseSummary.newBuilder()
                .setMessageId(messageCommon.getMessageId())
                .setMessageTypeId(messageCommon.getMessageTypeId())
                .setAggregatorContainerId(messageCommon.getInstance())
                .setMicroServiceId(ServiceNamesConstant.CRYPTO_VALIDATOR)
                .addAllValidationResponseList(validationResponseList)
                .build()
                ;
    }

    @Override
    public boolean sendMessageToAggregator(MessageCommon messageCommon) {

        ValidationResponseSummary validationResponseSummary
                = createFinalResponseMessage(messageCommon);

        boolean result = eventProcessors.aggregatorProcessor.processMessage(validationResponseSummary,
                messageCommon.getInstance());

        if (result) {
            removeAllCache(messageCommon);
        }
        return result;
    }

    @Override
    public MessageCommon processHSMResponse(HSMMessageAggregate hsmMessageAggregate) {

        MessageCommon messageCommon = cacheService.fetchCommonCache(hsmMessageAggregate.getMessageId());
        if (messageCommon != null) {
            messageCommon.getHsmMessageAggregateList()
                    .add(hsmMessageAggregate);
            for (MessageStatus messageStatus : messageCommon.getStatusList()) {
                if (messageStatus.getMessageId().equals(hsmMessageAggregate.getGeneratedId())) {
                    messageStatus.setCompleted(true);
                    break;
                }
            }
            return messageCommon;


        } else {
            logger.error("############, No Matching Common Message for id {}", hsmMessageAggregate.getMessageId());
            return null;
        }
    }

    @Override
    public void removeAllCache(MessageCommon messageCommon) {


        cacheService.removeAll(messageCommon);

    }


    private ValidationResponse buildResponse(ServiceResponse serviceResponse, Map<String, String> serviceFieldMap, String service) {


        return ValidationResponse.newBuilder()
                .setServiceId(service)
                .addAllValidationResponse(Arrays.asList(serviceResponse.getServiceResponse()))
                .putAllServiceResponseFields(serviceFieldMap)
                .build();


    }
}
