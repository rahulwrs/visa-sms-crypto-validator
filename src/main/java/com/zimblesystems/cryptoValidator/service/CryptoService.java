package com.zimblesystems.cryptoValidator.service;


import com.zimblesystems.cryptoValidator.model.ChipData;
import com.zimblesystems.cryptoValidator.model.CryptoData;
import com.zimblesystems.cryptoValidator.model.entity.CryptoProductDef;
import com.zimblesystems.cryptoValidator.model.hsm.ARQCModel;
import com.zimblesystems.cryptoValidator.model.hsm.CVCModel;
import com.zimblesystems.cryptoValidator.model.hsm.DynamicCvcModel;
import com.zimblesystems.cryptoValidator.model.hsm.PinModel;
import com.zimblesystems.cryptoValidator.model.proto.aggregator.ValidationResponseSummary;
import com.zimblesystems.cryptoValidator.model.proto.crypto.ChipInfo;
import com.zimblesystems.cryptoValidator.model.proto.crypto.CryptoValidator;
import in.nmaloth.payments.constants.ServiceResponse;
import io.smallrye.mutiny.Multi;

import java.util.Optional;

public interface CryptoService {

    Multi<CryptoProductDef> loadAllProducts();


    void loadAllProductsBlock();
    CryptoData convertProto(CryptoValidator cryptoValidator);
//    Mono<CryptoData> populateInstrumentCryptoInfo(CryptoData cryptoData);
    ChipData populateChipInfo(ChipInfo chipInfo);
    boolean validateCryptoData(CryptoData cryptoData);
    boolean filterForCvv(CryptoData cryptoData);
    boolean filterForCvv2(CryptoData cryptoData);
    boolean filterForPin(CryptoData cryptoData);
    boolean filterForChip(CryptoData cryptoData);
    boolean filterForCavv(CryptoData cryptoData);
    Optional<ServiceResponse> validateForCVC(CryptoData cryptoData);
    Optional<ServiceResponse> validateForPin(CryptoData cryptoData);
    Optional<ServiceResponse> validateForARQC(CryptoData cryptoData);
    Optional<ServiceResponse> validateForCavv(CryptoData cryptoData);
    Optional<ServiceResponse> validateForCVV2(CryptoData cryptoData);
    Optional<ServiceResponse> validateForDynamicCVV2(CryptoData cryptoData);
    ARQCModel extractARQCValidationData(CryptoData cryptoData);
    PinModel extractPinVerificationData(CryptoData cryptoData);
    CVCModel extractCVCVerificationData(CryptoData cryptoData);
    CVCModel extractCVC2VerificationData(CryptoData cryptoData);
    DynamicCvcModel extractDynamicCVCVerificationData(CryptoData cryptoData);
    ValidationResponseSummary createErrorResponse(String serviceId, ServiceResponse serviceResponse, CryptoData cryptoData);
    CVCModel extractCavvVerificationData(CryptoData cryptoData);
    void  sendMessageToHSM(CryptoData cryptoData);











}
