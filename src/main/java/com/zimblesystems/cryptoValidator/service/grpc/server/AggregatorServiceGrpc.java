package com.zimblesystems.cryptoValidator.service.grpc.server;

import com.zimblesystems.cryptoValidator.config.EventBusNames;
import com.zimblesystems.cryptoValidator.config.EventProcessors;
import com.zimblesystems.cryptoValidator.config.ServiceNames;
import com.zimblesystems.cryptoValidator.listeners.MessageListener;
import com.zimblesystems.cryptoValidator.listeners.MessageListenerImpl;
import com.zimblesystems.cryptoValidator.model.proto.aggregator.*;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.Vertx;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@GrpcService
public class AggregatorServiceGrpc implements AggregatorService {

    private static final Logger logger = LoggerFactory.getLogger(AggregatorServiceGrpc.class);

    @Inject
    EventProcessors eventProcessors;
    @Inject
    Vertx vertx;


    @Override
    public Multi<ValidationResponseSummary> aggregatorStream(Multi<AggregatorResponse> request) {

        MessageListener<ValidationResponseSummary> messageListener = new MessageListenerImpl<>();

        request.onItem().invoke(aggregatorResponse -> {

                    if (aggregatorResponse.hasRegistration()) {
                        logger.info("############### cryptoValidator: Aggregator has Registration :- {}", aggregatorResponse.toString());
                        messageListener.setServiceName(aggregatorResponse.getRegistration().getServiceName());
                        messageListener.setServiceInstance(aggregatorResponse.getRegistration().getServiceInstance());

                        eventProcessors.aggregatorProcessor.processMessage(ValidationResponseSummary.newBuilder()
                                .setMessageId(aggregatorResponse.getMessageId())
                                .setRegistration(RegistrationAggregator.newBuilder()
                                        .setServiceName(ServiceNames.CRYPTO_SERVICE)
                                        .setServiceInstance(eventProcessors.INSTANCE)
                                        .build()).build(), aggregatorResponse.getRegistration().getServiceInstance());

                        StatusUpdateAggregator statusUpdateAggregator = StatusUpdateAggregator.newBuilder()
                                .setServiceInstance(eventProcessors.INSTANCE)
                                .setServiceName(ServiceNames.CRYPTO_SERVICE)
                                .setReadyStatus(true)
                                .build();
                        eventProcessors.aggregatorProcessor.processMessage(ValidationResponseSummary.newBuilder()
                                .setMessageId(UUID.randomUUID().toString().replace("-",""))
                                .setStatusUpdateAggregator(statusUpdateAggregator)
                                .build(),aggregatorResponse.getRegistration().getServiceInstance()

                        );

                    }

                    if(aggregatorResponse.hasStatusUpdateAggregator()){
                        logger.info("############### cryptoValidator: Aggregator has Status Update :- {}", aggregatorResponse.toString());
                        eventProcessors.aggregatorProcessor.updateReadyStatus(aggregatorResponse.getStatusUpdateAggregator().getServiceInstance(),
                                aggregatorResponse.getStatusUpdateAggregator().getServiceName(),aggregatorResponse.getStatusUpdateAggregator().getReadyStatus());
                    }
                })
                .onFailure().invoke(throwable -> {
                    throwable.printStackTrace();
                })
                .onCancellation().invoke(() -> logger.info(" Cancelled the Stream {} for instance {}", messageListener.getServiceName(), messageListener.getServiceInstance()))
                .onTermination().invoke(() -> {

                    messageListener.getEmitter().complete();
                    eventProcessors.aggregatorProcessor.removeRegisteredFluxListener(messageListener);

                })
                .filter(aggregatorResponse -> !(aggregatorResponse.hasRegistration()|| aggregatorResponse.hasStatusUpdateAggregator()))
                .subscribe().with(aggregatorResponse -> {

                    vertx.eventBus().send(EventBusNames.AGGREGATOR_RESPONSE, aggregatorResponse);
                    // update cache for response
                });

        return getAggregatorMulti(messageListener);

    }

    private Multi<ValidationResponseSummary> getAggregatorMulti(MessageListener<ValidationResponseSummary> messageListener) {
        return Multi.createFrom().<ValidationResponseSummary>emitter(multiEmitter -> {

                    messageListener.setEmitter(multiEmitter);
                    eventProcessors.aggregatorProcessor.registerFluxListeners(messageListener);

                }).onFailure().invoke(() -> {
                    logger.info("Removed the listener {} for instance ", messageListener.toString(), messageListener.getServiceInstance());

                }).onCancellation().invoke(() -> logger.info(" Cancelled Stream for {} and Instance {}", messageListener.getServiceName(), messageListener.getServiceInstance()))
                .onTermination().invoke(() -> {
                    logger.info("Terminated the listener {} for instance {}", messageListener.toString(), messageListener.getServiceInstance());
                    eventProcessors.aggregatorProcessor.removeRegisteredFluxListener(messageListener);
                })

                ;
    }
}
