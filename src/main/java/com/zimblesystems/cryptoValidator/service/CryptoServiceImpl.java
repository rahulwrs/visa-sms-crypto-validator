package com.zimblesystems.cryptoValidator.service;


import com.zimblesystems.cryptoValidator.config.EventProcessors;
import com.zimblesystems.cryptoValidator.exception.HSMKeyNotFoundException;
import com.zimblesystems.cryptoValidator.model.ChipData;
import com.zimblesystems.cryptoValidator.model.CryptoData;
import com.zimblesystems.cryptoValidator.model.ProductId;
import com.zimblesystems.cryptoValidator.model.entity.CryptoProductDef;
import com.zimblesystems.cryptoValidator.model.hsm.ARQCModel;
import com.zimblesystems.cryptoValidator.model.hsm.CVCModel;
import com.zimblesystems.cryptoValidator.model.hsm.DynamicCvcModel;
import com.zimblesystems.cryptoValidator.model.hsm.PinModel;
import com.zimblesystems.cryptoValidator.model.proto.aggregator.ValidationResponse;
import com.zimblesystems.cryptoValidator.model.proto.aggregator.ValidationResponseSummary;
import com.zimblesystems.cryptoValidator.model.proto.crypto.ChipInfo;
import com.zimblesystems.cryptoValidator.model.proto.crypto.CryptoValidator;
import com.zimblesystems.cryptoValidator.service.hsm.HSMMessageService;
import in.nmaloth.payments.constants.EntryMode;
import in.nmaloth.payments.constants.RecurringTrans;
import in.nmaloth.payments.constants.ServiceResponse;
import in.nmaloth.payments.constants.ids.ServiceID;
import in.nmaloth.payments.constants.ids.ServiceNamesConstant;
import in.nmaloth.payments.constants.network.NetworkType;
import in.nmaloth.payments.keys.constants.CVCType;
import in.nmaloth.payments.keys.constants.PinBlockFormat;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.validation.ValidationException;
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class CryptoServiceImpl implements CryptoService {

    private final SchemeCryptoService schemeCryptoService;
    private final HSMMessageService hsmMessageService;
    private final EventProcessors eventProcessors;
    private final CacheService cacheService;

    private final Map<ProductId, CryptoProductDef> cryptoProductDefMap = new HashMap<>();


    public CryptoServiceImpl(
            SchemeCryptoService schemeCryptoService,
            HSMMessageService hsmMessageService,
            EventProcessors eventProcessors,
            CacheService cacheService) {

        this.schemeCryptoService = schemeCryptoService;
        this.hsmMessageService = hsmMessageService;
        this.eventProcessors = eventProcessors;
        this.cacheService = cacheService;
    }


    public void startup(@Observes StartupEvent startupEvent) {

        loadAllProductsBlock();

    }

    @Override
    public Multi<CryptoProductDef> loadAllProducts() {
        return CryptoProductDef.<CryptoProductDef>findAll().stream()
                .onItem().invoke(cryptoProductDef ->cryptoProductDefMap.put(new ProductId(cryptoProductDef.getOrg(), cryptoProductDef.getProduct()), cryptoProductDef))

                ;
    }

    @Override
    public void loadAllProductsBlock() {

        CryptoProductDef.<CryptoProductDef>listAll().await().indefinitely()
                .forEach(cryptoProductDef -> cryptoProductDefMap.put(new ProductId(cryptoProductDef.getOrg(), cryptoProductDef.getProduct()), cryptoProductDef));
    }

    @Override
    public CryptoData convertProto(CryptoValidator cryptoValidator) {


        CryptoData cryptoData = new CryptoData();
        cryptoData.setMessageId(cryptoValidator.getMessageId());
        cryptoData.setMessageTypeId(cryptoValidator.getMessageTypeId());
        cryptoData.setAggregatorInstance(cryptoValidator.getAggregatorInstance());
        cryptoData.setInstrument(cryptoValidator.getInstrument());
        cryptoData.setZoneKeyIndex(cryptoValidator.getZoneKeyIndex());
        cryptoData.setPinBlockFormat(PinBlockFormat.identify(cryptoValidator.getPinBlockFormat()));
        cryptoData.setZone(cryptoValidator.getZone());
        cryptoData.setEntryMode(EntryMode.identify(cryptoValidator.getEntryMode()));
        cryptoData.setCryptoOrg(cryptoValidator.getCryptoOrg());
        cryptoData.setCryptoProduct(cryptoValidator.getCryptoProduct());
        cryptoData.setZone(cryptoValidator.getZone());
        cryptoData.setNetworkType(NetworkType.identify(cryptoValidator.getNetworkId()));
        if (cryptoValidator.hasPinOffset()) {
            cryptoData.setPinOffset(cryptoValidator.getPinOffset());
        }
        if (cryptoValidator.hasPinLength()) {
            cryptoData.setPinLength(cryptoValidator.getPinLength());
        }

        if (cryptoValidator.hasDynamicCVV()) {
            cryptoData.setDynamicCVV(cryptoValidator.getDynamicCVV());
        }

        if (cryptoValidator.hasChipSeq()) {
            cryptoData.setChipSeq(cryptoValidator.getChipSeq());
        }

        if (cryptoValidator.hasChipVersion()) {
            cryptoData.setChipVersion(cryptoValidator.getChipVersion());
        }

        if (cryptoValidator.hasIadFormat()) {
            cryptoData.setIadFormat(cryptoValidator.getIadFormat());
        }
        if (cryptoValidator.hasAtc()) {
            cryptoData.setAtc(cryptoValidator.getAtc());
        }

        if (cryptoValidator.hasPinBlockFormat()) {
            cryptoData.setPinBlock(cryptoValidator.getPinBlock());
        }
        if (cryptoValidator.hasCavvResult()) {
            cryptoData.setCavvResult(cryptoValidator.getCavvResult());
        }

        if (cryptoValidator.hasOnlineCamResult()) {
            cryptoData.setOnlineCamResult(cryptoValidator.getOnlineCamResult());
        }
        if (cryptoValidator.hasCvvICvvResult()) {
            cryptoData.setCvv_ICvvResult(cryptoValidator.getCvvICvvResult());
        }
        if (cryptoValidator.hasCvv2Result()) {
            cryptoData.setCvv2Result(cryptoValidator.getCvv2Result());
        }
        if (cryptoValidator.hasChipInfo()) {

            cryptoData.setChipData(populateChipInfo(cryptoValidator.getChipInfo()));
        }
        if (cryptoValidator.hasCvv()) {
            cryptoData.setCvv(cryptoValidator.getCvv());
        }

        if (cryptoValidator.hasExpiryDate()) {
            cryptoData.setExpiryDate(getExpiryDate(cryptoValidator.getExpiryDate()));
        }
        if (cryptoValidator.hasServiceCode()) {
            cryptoData.setServiceCode(cryptoValidator.getServiceCode());
        }

        if (cryptoValidator.hasCvv2()) {
            cryptoData.setCvv2(cryptoValidator.getCvv2());
        }

        if (cryptoValidator.hasCavv()) {
            cryptoData.setCavv(cryptoValidator.getCavv().toByteArray());
        }
        if (cryptoValidator.hasRecurringTrans()) {
            cryptoData.setRecurringTrans(RecurringTrans.identify(cryptoValidator.getRecurringTrans()));
        }
        cryptoData.setSchemeId(schemeCryptoService.getThalesSchemeId());


        return cryptoData;
    }
//
//    @Override
//    public Mono<CryptoData> populateInstrumentCryptoInfo(CryptoData cryptoData) {
//
//        return instrumentCryptoDataService.findInstrumentCrypto(cryptoData.getInstrument())
//                .map(instrumentCryptoOptional -> {
//                    if (instrumentCryptoOptional.isPresent()) {
//
//                        populateInstrumentInfo(instrumentCryptoOptional.get(), cryptoData);
//                        return cryptoData;
//                    }
//                    throw new RuntimeException("Instrument Crypto Data Not Present .. ");
//                });
//    }
//
//    private void populateInstrumentInfo(InstrumentCrypto instrumentCrypto, CryptoData cryptoData) {
//
//        cryptoData.setCryptoOrg(instrumentCrypto.getCryptoOrg());
//        cryptoData.setCryptoProduct(instrumentCrypto.getCryptoProduct());
//
//        if (cryptoData.getExpiryDate() == null) {
//            return;
//        }
//        for (CryptoInfo cryptoInfo : instrumentCrypto.getCryptoInfoList()) {
//            if (cryptoInfo.getExpiryDate().isEqual(cryptoData.getExpiryDate())) {
//                cryptoData.setCryptoInfo(cryptoInfo);
//                break;
//            }
//        }
//
//        if (cryptoData.getChipData() == null) {
//            return;
//        }
//
//        int panSeq = 0;
//        if (cryptoData.getChipData().getPanSeqNumber() != null) {
//            panSeq = Integer.parseInt(cryptoData.getChipData().getPanSeqNumber());
//        }
//        for (ChipInfo chipInfo : instrumentCrypto.getChipInfoList()) {
//            if (panSeq == chipInfo.getChipSeq()) {
//                cryptoData.setChipInfo(chipInfo);
//                break;
//            }
//        }
//    }

    @Override
    public ChipData populateChipInfo(ChipInfo chipInfo) {

        ChipData chipData = new ChipData();

        if (chipInfo.hasTransactionDate()) {
            chipData.setTransactionDate(chipInfo.getTransactionDate());
        } else {
            chipData.setTransactionDate("000000");
        }

        if (chipInfo.hasRequestCryptogram()) {
            chipData.setRequestCryptogram(chipInfo.getRequestCryptogram());
        } else {
            chipData.setRequestCryptogram("0000000000000000");
        }

        if (chipInfo.hasTransactionAmount()) {
            chipData.setTransactionAmount(chipInfo.getTransactionAmount());
        } else {
            chipData.setTransactionAmount("000000000000");
        }

        if (chipInfo.hasUnpredictableNumber()) {
            chipData.setUnpredictableNumber(chipInfo.getUnpredictableNumber());
        } else {
            chipData.setUnpredictableNumber("00000000");
        }

        if (chipInfo.hasAip()) {
            chipData.setAip(chipInfo.getAip());
        } else {
            chipData.setAip("0000");
        }
        if (chipInfo.hasIad()) {
            chipData.setIad(chipInfo.getIad());
        } else {
            chipData.setIad("0000");
        }

        if (chipInfo.hasTvr()) {
            chipData.setTvr(chipInfo.getTvr());
        } else {
            chipData.setTvr("00000000");
        }

        if (chipInfo.hasAtc()) {
            chipData.setAtc(chipInfo.getAtc());
        } else {
            chipData.setAtc("0000");
        }

        if (chipInfo.hasTransactionCurrency()) {
            chipData.setTransactionCurrency(chipInfo.getTransactionCurrency());
        } else {
            chipData.setTransactionCurrency("0000");
        }
        if (chipInfo.hasPanSeqNumber()) {
            chipData.setPanSeqNumber(chipInfo.getPanSeqNumber());
        } else {
            chipData.setPanSeqNumber("00");
        }

        if (chipInfo.hasOtherAmount()) {
            chipData.setOtherAmount(chipInfo.getOtherAmount());
        } else {
            chipData.setOtherAmount("000000000000");
        }

        if (chipInfo.hasTransactionType()) {
            chipData.setTransactionType(chipInfo.getTransactionType());
        } else {
            chipData.setTransactionType("00");
        }

        if (chipInfo.hasTerminalCountryCode()) {
            chipData.setTerminalCountryCode(chipInfo.getTerminalCountryCode());
        } else {
            chipData.setTerminalCountryCode("0000");
        }

        return chipData;
    }

    @Override
    public boolean validateCryptoData(CryptoData cryptoData) {

        if (filterForCvv(cryptoData)) {
            Optional<ServiceResponse> serviceResponseOptional = validateForCVC(cryptoData);
            if (serviceResponseOptional.isPresent()) {
                eventProcessors.aggregatorProcessor.processMessage(
                        createErrorResponse(ServiceID.CVC_SERVICE, serviceResponseOptional.get(), cryptoData), cryptoData.getAggregatorInstance());
                return false;
            }
        }
        if (filterForCvv2(cryptoData)) {
            Optional<ServiceResponse> serviceResponseOptional = validateForCVV2(cryptoData);
            if (serviceResponseOptional.isPresent()) {
                eventProcessors.aggregatorProcessor.processMessage(
                        createErrorResponse(ServiceID.CVC2_SERVICE, serviceResponseOptional.get(), cryptoData), cryptoData.getAggregatorInstance());
                return false;
            }
        }
        if (filterForPin(cryptoData)) {
            Optional<ServiceResponse> serviceResponseOptional = validateForPin(cryptoData);
            if (serviceResponseOptional.isPresent()) {
                eventProcessors.aggregatorProcessor.processMessage(
                        createErrorResponse(ServiceID.PIN_SERVICE, serviceResponseOptional.get(), cryptoData), cryptoData.getAggregatorInstance());
                return false;
            }
        }

        if (filterForCavv(cryptoData)) {
            Optional<ServiceResponse> serviceResponseOptional = validateForCavv(cryptoData);
            if (serviceResponseOptional.isPresent()) {
                eventProcessors.aggregatorProcessor.processMessage(
                        createErrorResponse(ServiceID.CAVV_SERVICE, serviceResponseOptional.get(), cryptoData), cryptoData.getAggregatorInstance());
                return false;
            }
        }

        if (filterForChip(cryptoData)) {
            Optional<ServiceResponse> serviceResponseOptional = validateForARQC(cryptoData);
            if (serviceResponseOptional.isPresent()) {
                eventProcessors.aggregatorProcessor.processMessage(
                        createErrorResponse(ServiceID.ARQC_SERVICE, serviceResponseOptional.get(), cryptoData), cryptoData.getAggregatorInstance());
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean filterForCvv(CryptoData cryptoData) {

        return cryptoData.getCvv() != null;
    }

    @Override
    public boolean filterForCvv2(CryptoData cryptoData) {
        return cryptoData.getCvv2() != null;
    }

    @Override
    public boolean filterForPin(CryptoData cryptoData) {
        return cryptoData.getPinBlock() != null;
    }

    @Override
    public boolean filterForChip(CryptoData cryptoData) {
        return cryptoData.getChipData() != null;
    }

    @Override
    public boolean filterForCavv(CryptoData cryptoData) {
        return cryptoData.getCavv() != null;
    }

    @Override
    public Optional<ServiceResponse> validateForCVC(CryptoData cryptoData) {

        if (cryptoData.getServiceCode() == null) {
            return Optional.of(ServiceResponse.INVALID_SERVICE_CODE);
        }
//        if (cryptoData.getServiceCode().equals(cryptoData.getServiceCode())) {
//        } else {
//            return Optional.of(ServiceResponse.INVALID_SERVICE_CODE);
//        }
        return Optional.empty();
    }

    @Override
    public Optional<ServiceResponse> validateForPin(CryptoData cryptoData) {

//        if (cryptoData.getCryptoInfo() == null) {
//            return Optional.of(ServiceResponse.INVALID_EXPIRY_DATE);
//        }
        if (cryptoData.getPinBlockFormat() == null) {
            return Optional.of(ServiceResponse.INVALID_PIN_BLOCK_FORMAT);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ServiceResponse> validateForARQC(CryptoData cryptoData) {

        if (cryptoData.getChipVersion() == null || cryptoData.getChipSeq() == null || cryptoData.getIadFormat() == null) {
            return Optional.of(ServiceResponse.INVALID_CHIP);
        }
        if (schemeCryptoService.validateIad(cryptoData.getChipData().getIad(), cryptoData.getIadFormat())) {

        } else {
            return Optional.of(ServiceResponse.INVALID_IAD);
        }

        return Optional.empty();
    }

    @Override
    public Optional<ServiceResponse> validateForCavv(CryptoData cryptoData) {
        return Optional.empty();
    }

    @Override
    public Optional<ServiceResponse> validateForCVV2(CryptoData cryptoData) {

//        if (cryptoData.getCryptoInfo() == null) {
//            return Optional.of(ServiceResponse.INVALID_EXPIRY_DATE);
//        }

        if (cryptoData.getExpiryDate() == null) {
            return Optional.of(ServiceResponse.INVALID_EXPIRY_DATE);
        }
        if (cryptoData.getDynamicCVV() != null && cryptoData.getDynamicCVV()) {
            return validateForDynamicCVV2(cryptoData);
        }

        return Optional.empty();
    }

    @Override
    public Optional<ServiceResponse> validateForDynamicCVV2(CryptoData cryptoData) {

        Optional<Long> dynamicCvvTimeInterval = ConfigProvider.getConfig().getOptionalValue("crypto.dynamic.cvv.interval", Long.class);

        Optional<Integer> dynamicCvvTries = ConfigProvider.getConfig().getOptionalValue("crypto.dynamic.cvv.tries", Integer.class);

        if (dynamicCvvTimeInterval.isEmpty()) {
            return Optional.of(ServiceResponse.DCVV_NOT_SUPPORTED);
        } else if (dynamicCvvTimeInterval.get() == 0) {
            return Optional.of(ServiceResponse.DCVV_NOT_SUPPORTED);
        }

        return Optional.empty();
    }

    @Override
    public ARQCModel extractARQCValidationData(CryptoData cryptoData) {

        ChipData chipData = cryptoData.getChipData();

        ARQCModel arqcModel = new ARQCModel();

        arqcModel.setOrg(cryptoData.getCryptoOrg());
        arqcModel.setProduct(cryptoData.getCryptoProduct());
        arqcModel.setPan(cryptoData.getInstrument());
        arqcModel.setPanSeq(chipData.getPanSeqNumber());
        arqcModel.setAtc(chipData.getAtc());
        arqcModel.setUnpredictableNumber(chipData.getUnpredictableNumber());
        arqcModel.setArqc(chipData.getRequestCryptogram());
        arqcModel.setTransactionData(schemeCryptoService.buildTransactionData(chipData, cryptoData.getIadFormat(), cryptoData.getInstrument()));
        arqcModel.setArc("00");
        arqcModel.setSchemeId(cryptoData.getSchemeId());
        arqcModel.setCvn((schemeCryptoService.identifyCvn(chipData.getIad(), cryptoData.getIadFormat())));
//                .setGenerateARPC(true)
//                .setValidate(true)
        arqcModel.setZone(schemeCryptoService.identifyDki(chipData.getIad(), cryptoData.getIadFormat()));
//                .cvn(schemeCryptoService.identifyCvn(chipData.getIad(), cryptoData.getChipInfo().getIadFormat()))

        return arqcModel;
    }


    @Override
    public PinModel extractPinVerificationData(CryptoData cryptoData) {


        PinModel pinModel = new PinModel();
        pinModel.setOrg(cryptoData.getCryptoOrg());
        pinModel.setProduct(cryptoData.getCryptoProduct());
        pinModel.setZone(cryptoData.getZone());
        pinModel.setInstrument(cryptoData.getInstrument());
        pinModel.setPinOffset(cryptoData.getPinOffset());
        pinModel.setPin(cryptoData.getPinBlock());
        pinModel.setPinBlkFmt(cryptoData.getPinBlockFormat());
        pinModel.setPinLength(cryptoData.getPinLength());

        return pinModel;


    }

    @Override
    public CVCModel extractCVCVerificationData(CryptoData cryptoData) {

        String expiryDate = cryptoData.getExpiryDate().format(DateTimeFormatter.ofPattern("yyMM"));
        return populateCVCData(cryptoData.getServiceCode(), cryptoData.getCryptoOrg(), cryptoData.getCryptoProduct(), expiryDate, cryptoData.getCvv(), cryptoData.getInstrument(), CVCType.CVC);
    }

    private CVCModel populateCVCData(String serviceCode, int org, int product,
                                     String expiryDate, String cvv,
                                     String instrument, CVCType cvcType) {

        CVCModel cvcModel = new CVCModel();
        cvcModel.setOrg(org);
        cvcModel.setProduct(product);
        cvcModel.setInstrument(instrument);
        cvcModel.setExpiryDate(expiryDate);
        cvcModel.setServiceCode(serviceCode);
        cvcModel.setCvc(cvv);
        cvcModel.setCvcType(cvcType);
        return cvcModel;

    }

    @Override
    public CVCModel extractCVC2VerificationData(CryptoData cryptoData) {

        String expiryDate = cryptoData.getExpiryDate().format(DateTimeFormatter.ofPattern("yyMM"));
        return populateCVCData("000", cryptoData.getCryptoOrg(), cryptoData.getCryptoProduct(), expiryDate, cryptoData.getCvv2(), cryptoData.getInstrument(), CVCType.CVC2);
    }

    @Override
    public DynamicCvcModel extractDynamicCVCVerificationData(CryptoData cryptoData) {

        List<CVCModel> cvcDataList = new ArrayList<>();


        Optional<Long> dynamicCvvTimeInterval = ConfigProvider.getConfig().getOptionalValue("crypto.dynamic.cvv.interval", Long.class);

        Optional<Integer> dynamicCvvTries = ConfigProvider.getConfig().getOptionalValue("crypto.dynamic.cvv.tries", Integer.class);

        Long epochInterval = System.currentTimeMillis() / dynamicCvvTimeInterval.get();
        String pan = cryptoData.getInstrument().substring(cryptoData.getInstrument().length() - 8);

        String expiryDate = cryptoData.getExpiryDate().format(DateTimeFormatter.ofPattern("yyMM"));


        for (int i = 0; i < dynamicCvvTries.orElse(2); i++) {

            String modifiedPan = createModifiedPan((epochInterval).toString(), pan);
            CVCModel cvcModel = populateCVCData("888", cryptoData.getCryptoOrg(), cryptoData.getCryptoProduct(), expiryDate, cryptoData.getCvv2(), modifiedPan, CVCType.DYNAMIC_CVC2);
            cvcModel.setZone("00");
            cvcDataList.add(cvcModel);
            epochInterval--;
        }

        DynamicCvcModel dynamicCvcModel = new DynamicCvcModel();

        dynamicCvcModel.setCvcModelList(cvcDataList);
        dynamicCvcModel.setNumberOfTries(dynamicCvvTries.orElse(2));

        return dynamicCvcModel;
    }

    @Override
    public ValidationResponseSummary createErrorResponse(String serviceId, ServiceResponse serviceResponse, CryptoData cryptoData) {
        List<ValidationResponse> validationResponseList =
                new ArrayList<>();

        Map<String, String> serviceFieldMap = new HashMap<>();

        validationResponseList.add(
                ValidationResponse.newBuilder()
                        .setServiceId(serviceId)
                        .addAllValidationResponse(Arrays.asList(serviceResponse.getServiceResponse()))
                        .putAllServiceResponseFields(serviceFieldMap)
                        .build()
        );

        return ValidationResponseSummary.newBuilder()
                .setMessageId(cryptoData.getMessageId())
                .setMessageTypeId(cryptoData.getMessageTypeId())
                .setAggregatorContainerId(cryptoData.getAggregatorInstance())
                .setMicroServiceId(ServiceNamesConstant.CRYPTO_VALIDATOR)
                .addAllValidationResponseList(validationResponseList)
                .build()
                ;
    }


    private String createModifiedPan(String tbn, String pan) {

        if (tbn.length() < 8) {

            tbn = new StringBuilder()
                    .append("0".repeat(8 - tbn.length()))
                    .append(tbn)
                    .toString();
        } else {
            if (tbn.length() > 8) {
                tbn = tbn.substring(tbn.length() - 8);
            }
        }

        return new StringBuilder()
                .append(tbn)
                .append(pan)
                .toString();
    }


    @Override
    public CVCModel extractCavvVerificationData(CryptoData cryptoData) {

        byte[] cavv = cryptoData.getCavv();
        return populateCVCData(schemeCryptoService.extractServiceCodesFromCavv(cavv),
                cryptoData.getCryptoOrg(),
                cryptoData.getCryptoProduct(),
                schemeCryptoService.extractExpiryDateFromCavv(cavv),
                schemeCryptoService.extractCavv(cavv),
                cryptoData.getInstrument(), CVCType.CVV_3D
        );
    }

    @Override
    public void sendMessageToHSM(CryptoData cryptoData) {

        List<byte[]> messageList = new ArrayList<>();

        hsmMessageService.createMessageCommon(cryptoData.getMessageId(),
                cryptoData.getAggregatorInstance(), cryptoData.getMessageTypeId(),
                cryptoData.getEntryMode());

        if (filterForCvv(cryptoData)) {

            CVCModel cvcModel = extractCVCVerificationData(cryptoData);
            cvcModel.setZone("00");
            try {
                messageList.add(hsmMessageService.validateCVC(cvcModel, cryptoData.getMessageId()));
            } catch (HSMKeyNotFoundException e) {
                eventProcessors.aggregatorProcessor.processMessage(createErrorResponse(ServiceID.CVC_SERVICE, ServiceResponse.KEY_NOT_FOUND,
                        cryptoData), cryptoData.getAggregatorInstance());
                return;
            }
        }
        if (filterForCvv2(cryptoData)) {

            if (cryptoData.getDynamicCVV() != null && cryptoData.getDynamicCVV()) {

                DynamicCvcModel dynamicCvcModel = extractDynamicCVCVerificationData(cryptoData);

                try {
                    messageList.addAll(hsmMessageService.validateCVCD(dynamicCvcModel, cryptoData.getMessageId()));
                } catch (HSMKeyNotFoundException e) {
                    eventProcessors.aggregatorProcessor.processMessage(createErrorResponse(ServiceID.CVC2_SERVICE,
                            ServiceResponse.KEY_NOT_FOUND, cryptoData), cryptoData.getAggregatorInstance());
                    return;
                }

            } else {
                CVCModel cvcModel = extractCVC2VerificationData(cryptoData);
                cvcModel.setZone("00");
                try {
                    messageList.add(hsmMessageService.validateCVC2(cvcModel, cryptoData.getMessageId()));
                } catch (HSMKeyNotFoundException e) {
                    eventProcessors.aggregatorProcessor.processMessage(createErrorResponse(ServiceID.CVC2_SERVICE,
                            ServiceResponse.KEY_NOT_FOUND, cryptoData), cryptoData.getAggregatorInstance());
                    return;
                }
            }

        }
        if (filterForPin(cryptoData)) {
            PinModel pinModel = extractPinVerificationData(cryptoData);
            pinModel.setZone("00");
            try {
                CryptoProductDef cryptoProductDef = cryptoProductDefMap.get(new ProductId(cryptoData.getCryptoOrg(), cryptoData.getCryptoProduct()));
                if (cryptoProductDef == null) {
                    throw new ValidationException("No Crypto product found");
                }
                messageList.add(hsmMessageService.validatePin(pinModel, cryptoData.getMessageId(), cryptoProductDef.getDecimalisationTable()));
            } catch (Exception e) {
                eventProcessors.aggregatorProcessor.processMessage(createErrorResponse(ServiceID.PIN_SERVICE,
                        ServiceResponse.KEY_NOT_FOUND, cryptoData), cryptoData.getAggregatorInstance());
                return;
            }
        }
        if (filterForCavv(cryptoData)) {

            CVCModel cvcModel = extractCavvVerificationData(cryptoData);
            try {
                messageList.add(hsmMessageService.validateCVC_3D(cvcModel, cryptoData.getMessageId()));
            } catch (HSMKeyNotFoundException e) {
                eventProcessors.aggregatorProcessor.processMessage(createErrorResponse(ServiceID.CAVV_SERVICE,
                        ServiceResponse.KEY_NOT_FOUND, cryptoData), cryptoData.getAggregatorInstance());
                return;
            }
        }

        if (filterForChip(cryptoData)) {
            ARQCModel arqcModel = extractARQCValidationData(cryptoData);
            try {
                messageList.add(hsmMessageService.validateARQCAndGenerateARPC(arqcModel, cryptoData.getMessageId()));
            } catch (HSMKeyNotFoundException e) {
                eventProcessors.aggregatorProcessor.processMessage(createErrorResponse(ServiceID.ARQC_SERVICE,
                        ServiceResponse.KEY_NOT_FOUND, cryptoData), cryptoData.getAggregatorInstance());
                return;
            }
        }
        if (messageList.size() == 0) {
            eventProcessors.aggregatorProcessor.processMessage(createErrorResponse(ServiceID.NO_VALIDATION,
                    ServiceResponse.OK, cryptoData), cryptoData.getAggregatorInstance());

            cacheService.removeCommonCache(cryptoData.getMessageId());

        } else {
            hsmMessageService.sendMessage(messageList);
        }

    }

    private LocalDate getExpiryDate(String expiryDate) {

        return LocalDate.parse(expiryDate, DateTimeFormatter.BASIC_ISO_DATE);
    }
}
