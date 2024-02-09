package com.zimblesystems.cryptoValidator.service.tcp;


import com.zimblesystems.cryptoValidator.config.EventBusNames;
import com.zimblesystems.cryptoValidator.config.ServiceNames;
import com.zimblesystems.cryptoValidator.model.hsm.ConnectDTO;
import com.zimblesystems.cryptoValidator.model.hsm.HSMResult;
import com.zimblesystems.cryptoValidator.service.CacheService;
import com.zimblesystems.cryptoValidator.serviceEvents.model.ServiceAction;
import com.zimblesystems.cryptoValidator.serviceEvents.model.ServiceEvent;
import com.zimblesystems.cryptoValidator.verticles.TCPClientVerticle;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
public class TCPClientServiceImpl implements TCPClientService {

    private static  final Logger logger = LoggerFactory.getLogger(TCPClientService.class);

    private final List<TCPClientVerticle> tcpClientVerticleList = new ArrayList<>();

    private AtomicInteger roundRobin = new AtomicInteger(0);

    private final CacheService cacheService;
    private final Vertx vertx;

    public TCPClientServiceImpl(CacheService cacheService, Vertx vertx) {
        this.cacheService = cacheService;
        this.vertx = vertx;
    }



    @Override
    public void createClient(String host, Integer port, int reconnectCount) {

        String id = UUID.randomUUID().toString();

        TCPClientVerticle tcpClientVerticle = new TCPClientVerticle(id, host, port, this, reconnectCount);

        vertx.deployVerticle(tcpClientVerticle)
                .onFailure().invoke(throwable -> {
                    logger.info(" #############Failed in TCP connection to HSM ");
                    vertx.eventBus().send(EventBusNames.SERVICE_EVENTS, createServiceEvent(ServiceAction.RECONNECT_TCP,host,port,reconnectCount));

                    tcpClientVerticleList.remove(tcpClientVerticle);
                    throwable.printStackTrace();
                })
                .subscribe().with(s -> {
                    logger.info(" Deploy of tcp Client to HSM Complete with id {}", id);
                    logger.info(s);
                    logger.info(tcpClientVerticle.deploymentID());
//                    tcpClientVerticle.updateDeploymentId(s);
                    tcpClientVerticleList.add(tcpClientVerticle);

                    for (int i = 0; i < 3; i ++ ){
                        vertx.eventBus().send(EventBusNames.SERVICE_EVENTS,ServiceEvent.builder()
                                .serviceAction(ServiceAction.DIAGNOSTIC_HSM)
                                .instance(id)
                                .build()
                        );
                    }

                });

        logger.info(" Deploy Code Executing....");


    }

    @Override
    public Uni<String> createClient( ConnectDTO connectDTO){

        String id = UUID.randomUUID().toString();
        TCPClientVerticle tcpClientVerticle = new TCPClientVerticle(id, connectDTO.getHost(), connectDTO.getPort(), this, 0);

        return vertx.deployVerticle(tcpClientVerticle)
                .onFailure().invoke(throwable -> {
                    logger.info(" #############Failed in TCP connection to HSM ");
                    tcpClientVerticleList.remove(tcpClientVerticle);
                    throwable.printStackTrace();
                })
                .onItem().invoke(() -> tcpClientVerticleList.add(tcpClientVerticle))
                ;

    }

    @Override
    public List<ConnectDTO> connectionsOfHSM(){

        return tcpClientVerticleList.stream().map(tcpClientVerticle -> {
            ConnectDTO connectDTO = new ConnectDTO();
            connectDTO.setHost(tcpClientVerticle.getHost());
            connectDTO.setPort(tcpClientVerticle.getPort());
            connectDTO.setId(tcpClientVerticle.getId());
            return connectDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteTCPVerticle(TCPClientVerticle tcpClientVerticle){
        tcpClientVerticleList.remove(tcpClientVerticle);
    }



    private ServiceEvent createServiceEvent(ServiceAction serviceAction, String host, int port, int reconnectCount){

        ServiceEvent serviceEvent = new ServiceEvent();

        serviceEvent.setServiceName(ServiceNames.HSM_TCP);
        serviceEvent.setServiceAction(serviceAction);
        serviceEvent.setHost(host);
        serviceEvent.setPort(port);
        serviceEvent.setAttempts(reconnectCount + 1);
        return serviceEvent;


    }

    @Override
    public void sendMessage(byte[] message, String id) {

        for (TCPClientVerticle tcpClientVericle : tcpClientVerticleList) {

            if (tcpClientVericle.getId().equals(id)) {
                tcpClientVericle.writeMessages(message);
                break;
            }

        }

    }

    @Override
    public void sendMessage(byte[] message) {

        Optional<TCPClientVerticle> tcpClientVerticleOptional = selectListener();
        if (tcpClientVerticleOptional.isPresent()) {
            TCPClientVerticle tcpClientVerticle = tcpClientVerticleOptional.get();
            tcpClientVerticle.writeMessages(message);
        } else {
            logger.error(" No Active TCP Connection ...");
        }

    }

    @Override
    public Uni<Void> closeConnections(String id) {

        for (TCPClientVerticle tcpClientVerticle : tcpClientVerticleList) {

            if (tcpClientVerticle.getId().equals(id)) {

                return vertx.undeploy(tcpClientVerticle.deploymentID())
                        .onItem().invoke(unused -> tcpClientVerticleList.remove(tcpClientVerticle))
                        .replaceWithVoid()

                ;
            }
        }

//        return tcpClientVerticle.closeConnection();
        return Uni.createFrom().voidItem();
    }


    @Override
    public List<TCPClientVerticle> getAllTCPVerticles() {
        return tcpClientVerticleList;
    }


    @Override
    public void sendMessageForIncomingTCPProcessing(byte[] message, String channelId) {

        HSMResult hsmResult = createHSMResult(message);
        cacheService.validateHSMResults(hsmResult);

    }

    private HSMResult createHSMResult(byte[] bytesMessage){


        String message = new String(bytesMessage);

        String header = message.substring(0,4);

        HSMResult hsmResult = new HSMResult();
        hsmResult.setId(header);
        hsmResult.setCommand(message.substring(4,6));
        hsmResult.setResponseCode(message.substring(6,8));
        hsmResult.setResponseMessage(message.substring(8));

        logger.info("###### HSM Result - Command/Resp/ Resp Message : {} / {} / {}", hsmResult.getCommand(), hsmResult.getResponseCode(),hsmResult.getResponseMessage());

        return hsmResult;

    }


    private Optional<TCPClientVerticle> selectListener() {
        int size = tcpClientVerticleList.size();
        if (size > 0) {
            int roundRobin = this.roundRobin.incrementAndGet();
            int selectedVerticle = roundRobin % size;
            if (roundRobin > 99999) {
                this.roundRobin = new AtomicInteger(0);
            }
            return Optional.ofNullable(tcpClientVerticleList.get(selectedVerticle));
        }
        return Optional.empty();
    }

    private byte[] integerToByte(int number, int byteArrayLength) {
        byte[] bytes = new byte[byteArrayLength];
        if (byteArrayLength == 1) {
            if (number < 256) {
                bytes[0] = (byte) (number);
                return bytes;
            } else {
                throw new IllegalArgumentException("Single Byte integer cannot have Length More than 255");
            }
        }
        if (byteArrayLength == 2) {
            bytes[1] = (byte) (number);
            bytes[0] = (byte) (number >> 8);

            return bytes;

        }
        if(byteArrayLength ==3){
            bytes[2] = (byte)(number);
            bytes[1] = (byte)(number>>8);
            bytes[0] = (byte)(number>>16);
            return bytes;
        }
        throw new IllegalArgumentException("Byte Array Length greater than 3 not supported for Integer");
    }



}
