package com.zimblesystems.cryptoValidator.verticles;

import com.zimblesystems.cryptoValidator.config.EventBusNames;
import com.zimblesystems.cryptoValidator.config.EventProcessors;
import com.zimblesystems.cryptoValidator.config.RequestNames;
import com.zimblesystems.cryptoValidator.config.ServiceNames;
import com.zimblesystems.cryptoValidator.service.grpc.client.DistributorMessageClient;
import com.zimblesystems.cryptoValidator.service.hsm.HSMMessageService;
import com.zimblesystems.cryptoValidator.service.tcp.TCPClientService;
import com.zimblesystems.cryptoValidator.serviceEvents.model.Connections;
import com.zimblesystems.cryptoValidator.serviceEvents.model.ServiceAction;
import com.zimblesystems.cryptoValidator.serviceEvents.model.ServiceEvent;
import com.zimblesystems.cryptoValidator.serviceEvents.model.discovery.ServerInfo;
import com.zimblesystems.cryptoValidator.serviceEvents.services.DependentServiceDiscovery;
import in.nmaloth.payments.constants.EntryMode;
import in.nmaloth.payments.constants.network.MessageType;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ServiceEventVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(ServiceEventVerticle.class);

    @ConfigProperty(name = "quarkus.test.mode")
    public Optional<Boolean> testModeOptional;

    @ConfigProperty(name = "grpc.reattempts")
    public Optional<Integer> grpcReAttemptTimes;

    @ConfigProperty(name = "service.discovery.retry")
    Optional<Integer> serviceDiscoveryRetry;

    @ConfigProperty(name = "service.discovery.server-present")
    Optional<Boolean> serverPresent;

    @ConfigProperty(name = "hsm.reattempts")
    public Optional<Integer> tcpServerReAttemptTimes;

    @Inject
    TCPClientService tcpClientService;

//    @Inject
//    AggregatorMessageClient aggregatorMessageClient;
    @Inject
    EventProcessors eventProcessors;
    @Inject
    DistributorMessageClient distributorMessageClient;
    @Inject
    HSMMessageService hsmMessageService;
    @Inject
    DependentServiceDiscovery dependentServiceDiscovery;

    private final List<Connections> connectionsList = new ArrayList<>();

    private final List<String> deleteConnectionList = new ArrayList<>();


    @Override
    public Uni<Void> asyncStart() {
        return vertx.eventBus().<ServiceEvent>consumer(EventBusNames.SERVICE_EVENTS)
                .handler(serviceEventMessage -> processServiceEvents(serviceEventMessage.body())).completionHandler()
                ;
    }

    private void processServiceEvents(ServiceEvent serviceEvent) {

        logger.info(" ####Service Event {}", serviceEvent.toString());

        if(serviceEvent.getAttempts() == null){
            serviceEvent.setAttempts(0);
        }

        switch (serviceEvent.getServiceAction()) {

            case SETUP_CLIENT: {

                Optional<Connections> connectionsOptional =  connectionsList.stream()
                        .filter(connections -> connections.getHost().equals(serviceEvent.getHost())&&
                                connections.getInstance().endsWith(serviceEvent.getInstance()) &&
                                connections.getServiceName().equals(serviceEvent.getServiceName()))
                        .findFirst();

                if(connectionsOptional.isEmpty()){

//                    Optional<String> deletedConnectionOptional = deleteConnectionList.stream()
//                            .filter(s -> s.equals(serviceEvent.getInstance()))
//                            .findFirst();

//                    if(deletedConnectionOptional.isEmpty()){
                        logger.info("##########Entered Setup Request Client ");

                        newConnectionServiceEvent(serviceEvent);
//                    }
                } else {
                    logger.info(" #############3 Managed Channel info #### {}",connectionsOptional.get().getManagedChannel());

                    if(connectionsOptional.get().getManagedChannel()  != null){
                        logger.info(" Channel Status is ... {} for state {}", connectionsOptional.get().getManagedChannel().isShutdown(),connectionsOptional.get().getManagedChannel().getState(true).name());
                    }
                }
                break;

            }
            case PROCESS_REQUEST: {
                logger.info("##########Entered Process Request Client ");

                if (serviceEvent.getServiceName().equals(ServiceNames.DISTRIBUTOR)) {
                    distributorMessageClient.createAggregatorStream(serviceEvent);
                }
//                } else {
//                    aggregatorMessageClient.createAggregatorStream(serviceEvent);
//                }
                break;
            }

            case REMOVE_CLIENT: {

                logger.info("##########Entered remove Client ");
                removeConnections(serviceEvent);
                break;
            }
            case SERVICE_DISCOVERY: {
                logger.info("##########Entered ServiceDiscovery Client ");

                serviceDiscovery(serviceEvent);
                break;
            }

            case SERVICE_DISCOVERY_ALL: {
                logger.info("############ Entered Service Discovery All");
                serviceDiscoveryAll();
                break;
            }
            case CHECK_STATUS_AVAILABLE: {

                if(eventProcessors.aggregatorProcessor.getReadyStatus()){
                    distributorMessageClient.sendUpdateStatus(serviceEvent.getInstance(),serviceEvent.getServiceName());
                } else {
                    checkStatus(serviceEvent);
                }
                break;
            }
            case CONNECT_TCP: {
                logger.info(" ################### Entered Connect TCP ");

                tcpClientService.createClient(serviceEvent.getHost(), serviceEvent.getPort(),0)

                ;
                break;

            }
            case RECONNECT_TCP: {

                logger.info("###############  Entered Reconnection Logic");

                if(serviceEvent.getAttempts() > tcpServerReAttemptTimes.orElse(2) ){
                    tcpClientService.createClient(serviceEvent.getHost(),serviceEvent.getPort(), serviceEvent.getAttempts());
                }

            }
            case REMOVE_TCP: {
                logger.info(" ############# Entering disconnect TCP ");
                tcpClientService
                        .closeConnections(serviceEvent.getInstance()).subscribe().with(unused -> {});
            }
/*R
            case ADD_WATCHER:{
                logger.info("############# Add Watchers for all  services");
                vertx.executeBlocking(Uni.createFrom().<String>emitter(uniEmitter -> {
                    dependentServiceDiscovery.addWatchers();
                    uniEmitter.complete("Added All Watchers");
                })).subscribe().with(message -> logger.info(message))
                ;
                break;
            }
            case ADD_WATCHER_SERVICE:{
                logger.info("############# Add Watchers for services {}", serviceEvent.getServiceName());

                vertx.executeBlocking(Uni.createFrom().<String>emitter(uniEmitter -> {
                    dependentServiceDiscovery.addWatchers(serviceEvent.getServiceName());
                    uniEmitter.complete(serviceEvent.getServiceName());
                })).subscribe().with(s -> logger.info("Added Watcher for service {} ",serviceEvent.getServiceName()))
                ;
                break;
            }

R*/
            case DIAGNOSTIC_HSM:{

                String messageId = UUID.randomUUID().toString();
                hsmMessageService.createMessageCommon(messageId,
                        eventProcessors.INSTANCE, MessageType.INITIAL_REQUEST.getMessageType(),
                        EntryMode.UNKNOWN);

               byte[] message = hsmMessageService.sendDiagnostics(messageId);

                hsmMessageService.sendMessage(message);

                break;

            }

            default: {

                break;
            }
        }
    }

    private void serviceDiscovery(ServiceEvent serviceEvent) {

        dependentServiceDiscovery.discoverServices(serviceEvent.getServiceName())
                .onItem().invoke(serverInfoList -> {
                    if (serverInfoList.size() == 0) {
                        vertx.executeBlocking(Uni.createFrom().<ServiceEvent>emitter(uniEmitter -> {

                            try {
                                Thread.sleep(serviceDiscoveryRetry.orElse(10000));
                                uniEmitter.complete(serviceEvent);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        })).subscribe().with(serviceEvent1 -> vertx.eventBus().send(EventBusNames.SERVICE_EVENTS, serviceEvent1));
                    }
                }).onItem().transformToMulti(serverInfoList -> Multi.createFrom().iterable(serverInfoList))
                .onItem().transform(this::createServiceEvent)
                .onFailure().invoke(Throwable::printStackTrace)
                .subscribe().with(serviceEvent1 -> vertx.eventBus().send(EventBusNames.SERVICE_EVENTS, serviceEvent1));
    }


    private ServiceEvent createServiceEvent(ServerInfo serverInfo){

        ServiceEvent.ServiceEventBuilder builder = ServiceEvent.builder()
                .serviceName(serverInfo.getServiceName())
                .instance(serverInfo.getServiceInstance())
                .host(serverInfo.getHost())
                .port(serverInfo.getPort())
                .attempts(0)
                .serviceAction(ServiceAction.SETUP_CLIENT);
        if (serverInfo.getServiceName().equals(ServiceNames.DISTRIBUTOR)) {
            builder.requestName(RequestNames.DISTRIBUTOR_STREAM_INCOMING);
        } else {
            builder.requestName(RequestNames.AGGREGATOR_STREAM_OUTGOING);
        }
        return builder.build();


    }

    private void checkStatus(ServiceEvent serviceEvent){

        vertx.executeBlocking(Uni.createFrom().<ServiceEvent>emitter(uniEmitter -> {

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            uniEmitter.complete(serviceEvent);

        })).subscribe().with(serviceEvent1 -> {
            if(eventProcessors.aggregatorProcessor.getReadyStatus()){
                distributorMessageClient.sendUpdateStatus(serviceEvent1.getInstance(),serviceEvent.getServiceName());
            }else {
                vertx.eventBus().send(EventBusNames.SERVICE_EVENTS,serviceEvent1);
            }

        })
        ;
    }

    private void removeConnections(ServiceEvent serviceEvent) {

        Optional<Connections> connectionsOptional = connectionsList.stream()
                .filter(connections -> connections.getServiceName().equalsIgnoreCase(serviceEvent.getServiceName()))
                .filter(connections -> connections.getInstance().equalsIgnoreCase(serviceEvent.getInstance()))
                .findFirst();

        connectionsList.forEach(connections -> logger.info("###########Connection {}", connections.toString()));

        if (connectionsOptional.isEmpty()) {
            logger.error(" ################## Invalid Service Event {}", serviceEvent.toString());
            return;
        }
        Connections connection = connectionsOptional.get();
        deleteConnectionList.add(serviceEvent.getInstance());
        connectionsList.remove(connection);
        connectionsList.forEach(connections -> logger.info("############## Connection Info After delete {}",connections.toString()));
        logger.info(" Are these Channels Shutdown {} or Terminated {}", connection.getManagedChannel().isShutdown(), connection.getManagedChannel().isTerminated());

        vertx.executeBlocking(Uni.createFrom().<ServiceEvent>emitter(uniEmitter -> {
            try {
                Thread.sleep(2000);

                logger.info(" Reconnecting ... {}",serviceEvent.toString());
//                if (serviceEvent.getAttempts() < grpcReAttemptTimes.orElse(2)) {
//
//                    uniEmitter.complete(ServiceEvent.builder()
//                            .serviceName(connection.getServiceName())
//                            .instance(connection.getInstance())
//                            .attempts(serviceEvent.getAttempts() + 1)
//                            .host(connection.getHost())
//                            .port(connection.getPort())
//                            .serviceAction(ServiceAction.SETUP_CLIENT)
//                            .build());
//                } else {

                logger.info(" #############Invoking Service Discovery");
//                    Thread.sleep(serviceDiscoveryRetry.orElse(10000));

                ServiceEvent serviceEvent1 = ServiceEvent.builder()
                        .serviceAction(ServiceAction.SERVICE_DISCOVERY)
                        .serviceName(serviceEvent.getServiceName())
                        .build();
                vertx.eventBus().send(EventBusNames.SERVICE_EVENTS,serviceEvent1);
//                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        })).subscribe().with(serviceEvent1 -> vertx.eventBus().send(EventBusNames.SERVICE_EVENTS, serviceEvent1))
        ;

    }


    private void serviceDiscoveryAll() {

        dependentServiceDiscovery.discoverAllDependentServices()
                .onItem().invoke(serverInfoList -> {

                            if (serverInfoList.size() == 0 && serverPresent.orElse(true)) {
                                vertx.executeBlocking(Uni.createFrom().<ServiceEvent>emitter(uniEmitter -> {

                                    try {
                                        Thread.sleep(serviceDiscoveryRetry.orElse(10000));
                                        ServiceEvent.builder().serviceAction(ServiceAction.SERVICE_DISCOVERY_ALL).build();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                })).subscribe().with(serviceEvent -> vertx.eventBus().send(EventBusNames.SERVICE_EVENTS, serviceEvent))
                                ;
                            }
                        }

                )
                .onFailure().invoke(Throwable::printStackTrace)
                .onItem().transformToMulti(serverInfoList -> Multi.createFrom().iterable(serverInfoList))
                .onItem().transform(this::createServiceEvent)
                .subscribe().with(serviceEvent -> vertx.eventBus().send(EventBusNames.SERVICE_EVENTS, serviceEvent));

    }

    private void newConnectionServiceEvent(ServiceEvent serviceEvent) {

        ManagedChannel channel = createNewChannel(serviceEvent);
        Connections connections = Connections.builder()
                .serviceName(serviceEvent.getServiceName())
                .host(serviceEvent.getHost())
                .port(serviceEvent.getPort())
                .instance(serviceEvent.getInstance())
                .managedChannel(channel)
                .build();

        connectionsList.add(connections);


        ServiceEvent serviceEvent1 = ServiceEvent.builder()
                .serviceAction(ServiceAction.PROCESS_REQUEST)
                .serviceAction(ServiceAction.PROCESS_REQUEST)
                .channel(channel)
                .requestName(serviceEvent.getRequestName())
                .serviceName(serviceEvent.getServiceName())
                .instance(serviceEvent.getInstance())
                .attempts(serviceEvent.getAttempts())
                .build();

        vertx.eventBus().send(EventBusNames.SERVICE_EVENTS, serviceEvent1);
    }

    private ManagedChannel createNewChannel(ServiceEvent serviceEvent) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(serviceEvent.getHost(), serviceEvent.getPort());

        if (testModeOptional.orElse(false)) {
            builder.usePlaintext();
        }
        return builder.build();
    }
}
