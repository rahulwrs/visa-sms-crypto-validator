package com.zimblesystems.cryptoValidator.service.grpc.client;


import com.zimblesystems.cryptoValidator.listeners.MessageListener;
import com.zimblesystems.cryptoValidator.model.proto.crypto.CryptoResponse;
import com.zimblesystems.cryptoValidator.serviceEvents.model.ServiceEvent;
import io.smallrye.mutiny.Multi;

public interface DistributorMessageClient {


    void createAggregatorStream(ServiceEvent serviceEvent);

    Multi<CryptoResponse> getOutgoingResponseMulti(MessageListener<CryptoResponse> messageListener);

    void sendUpdateStatus(String instance, String serviceName);

}
