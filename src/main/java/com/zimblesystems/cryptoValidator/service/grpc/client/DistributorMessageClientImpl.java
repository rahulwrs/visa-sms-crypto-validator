package com.zimblesystems.cryptoValidator.service.grpc.client;

import com.zimblesystems.cryptoValidator.config.EventBusNames;
import com.zimblesystems.cryptoValidator.config.EventProcessors;
import com.zimblesystems.cryptoValidator.config.RequestNames;
import com.zimblesystems.cryptoValidator.config.ServiceNames;
import com.zimblesystems.cryptoValidator.listeners.MessageListener;
import com.zimblesystems.cryptoValidator.listeners.MessageListenerImpl;

import com.zimblesystems.cryptoValidator.model.proto.crypto.CryptoResponse;
import com.zimblesystems.cryptoValidator.model.proto.crypto.MutinyCryptoServiceGrpc;
import com.zimblesystems.cryptoValidator.model.proto.crypto.RegistrationCrypto;
import com.zimblesystems.cryptoValidator.model.proto.crypto.StatusUpdateCrypto;
import com.zimblesystems.cryptoValidator.serviceEvents.model.ServiceAction;
import com.zimblesystems.cryptoValidator.serviceEvents.model.ServiceEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@ApplicationScoped
public class DistributorMessageClientImpl implements DistributorMessageClient {

    private static final Logger logger = LoggerFactory.getLogger(DistributorMessageClientImpl.class);

    @Inject
    EventProcessors eventProcessors;
    @Inject
    Vertx vertx;


    @Override
    public void createAggregatorStream(ServiceEvent serviceEvent) {

        MutinyCryptoServiceGrpc.MutinyCryptoServiceStub client = MutinyCryptoServiceGrpc.newMutinyStub(serviceEvent.getChannel());

        EventBus eventBus = vertx.eventBus();

        MessageListener<CryptoResponse> messageListener = new MessageListenerImpl<>();

        client.sendCryptoMessage(getOutgoingResponseMulti(messageListener))
                .onItem().invoke(cryptoValidator -> {

                    if (cryptoValidator.hasRegistration()) {
                        logger.info("############### cryptoValidator: Distributor has Registration :- {}", cryptoValidator.toString());
                        messageListener.setServiceName(cryptoValidator.getRegistration().getServiceName());
                        messageListener.setServiceInstance(cryptoValidator.getRegistration().getServiceInstance());

                        if (eventProcessors.aggregatorProcessor.getReadyStatus()) {

                            sendUpdateStatus(cryptoValidator.getRegistration().getServiceInstance(), cryptoValidator.getRegistration().getServiceName());

                        } else {

                            vertx.eventBus().send(EventBusNames.SERVICE_EVENTS, ServiceEvent.builder()
                                    .serviceAction(ServiceAction.CHECK_STATUS_AVAILABLE)
                                    .serviceName(RequestNames.AGGREGATOR_STREAM_OUTGOING)
                                    .instance(cryptoValidator.getRegistration().getServiceInstance())
                                    .build());

                        }

                    } else if (cryptoValidator.hasStatusUpdate()) {
                        logger.info("cryptoValidator: Distributor has Status Update : {}",cryptoValidator.toString());
                        eventProcessors.cryptoResponseProcessor.updateReadyStatus(cryptoValidator.getStatusUpdate().getServiceInstance(),
                                cryptoValidator.getStatusUpdate().getServiceName(), cryptoValidator.getStatusUpdate().getReadyStatus());
                    }
                })
                .onFailure().retry().atMost(1L)
                .onFailure().recoverWithMulti(() -> {
                    logger.info(" ################ Entered Termination");
                    eventProcessors.cryptoResponseProcessor.removeRegisteredFluxListener(messageListener);
                    return Multi.createFrom().emitter(multiEmitter -> multiEmitter.complete());

                })
                .onTermination().invoke(() -> {

                    logger.info(" ################ Entered Termination");
                    ServiceEvent serviceEvent1 = ServiceEvent.builder().serviceName(serviceEvent.getServiceName())
                            .requestName(RequestNames.DISTRIBUTOR_STREAM_INCOMING)
                            .serviceAction(ServiceAction.REMOVE_CLIENT)
                            .instance(serviceEvent.getInstance())
                            .attempts(serviceEvent.getAttempts())
                            .build();

                    eventProcessors.cryptoResponseProcessor.removeRegisteredFluxListener(messageListener);
                    logger.info(" Terminated Connection for Service {} Instance {} ", serviceEvent1.getServiceName(), serviceEvent1.getInstance());
                    eventBus.send(EventBusNames.SERVICE_EVENTS, serviceEvent1);

                })

                .filter(cryptoValidator -> !(cryptoValidator.hasRegistration() || cryptoValidator.hasStatusUpdate()))
                .subscribe().with(cryptoValidator -> {

                    logger.info("############### Message from Distributor Received : {}", cryptoValidator.getMessageId());
                    eventBus.send(EventBusNames.CRYPTO_BUS, cryptoValidator);

                });

    }


    @Override
    public Multi<CryptoResponse> getOutgoingResponseMulti(MessageListener<CryptoResponse> messageListener) {

        return Multi.createFrom()
                .<CryptoResponse>emitter(multiEmitter -> updateEventProcessors(multiEmitter, messageListener))
                .onFailure().invoke(throwable -> throwable.printStackTrace())

                ;
    }

    @Override
    public void sendUpdateStatus(String instance, String serviceName) {

        StatusUpdateCrypto statusUpdateCrypto = StatusUpdateCrypto.newBuilder()
                .setServiceInstance(eventProcessors.INSTANCE)
                .setServiceName(ServiceNames.CRYPTO_SERVICE)
                .setReadyStatus(true)
                .build();

        eventProcessors.cryptoResponseProcessor.processMessage(CryptoResponse.newBuilder()
                .setMessageId(UUID.randomUUID().toString().replace("-", ""))
                .setStatusUpdate(statusUpdateCrypto)
                .build(), instance

        );

        eventProcessors.cryptoResponseProcessor.updateReadyStatus(instance,
                serviceName, true);

    }

    private void updateEventProcessors(MultiEmitter<? super CryptoResponse> multiEmitter, MessageListener<CryptoResponse> messageListener) {

        messageListener.setServiceName(ServiceNames.DISTRIBUTOR);
//        messageListener.setServiceInstance(eventProcessors.INSTANCE);
        messageListener.setEmitter(multiEmitter);

        multiEmitter.emit(CryptoResponse.newBuilder()
                .setMessageId(UUID.randomUUID().toString().replace("-", ""))
                .setRegistration(RegistrationCrypto.newBuilder()
                        .setServiceName(ServiceNames.CRYPTO_SERVICE)
                        .setServiceInstance(eventProcessors.INSTANCE)
                        .build())
                .build());

        eventProcessors.cryptoResponseProcessor.registerFluxListeners(messageListener);

        logger.info(" ############### Registering Listeners  {}", messageListener.toString());

    }

}
