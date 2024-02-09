package com.zimblesystems.cryptoValidator.verticles;


import com.google.common.primitives.Bytes;
import com.zimblesystems.cryptoValidator.service.tcp.TCPClientService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetClientOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class TCPClientVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(TCPClientVerticle.class);

    private NetSocket netSocket;
    private final String id;
    private final String host;
    private final Integer port;
    private final TCPClientService tcpClientService;
    private byte[] bytes;
    private int reconnectCount;


    public TCPClientVerticle(String id, String host, Integer port, TCPClientService tcpClientService, int reconnectCount) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.tcpClientService = tcpClientService;
        this.reconnectCount = reconnectCount;
    }


    @Override
    public Uni<Void> asyncStart() {

        return vertx.createNetClient(new NetClientOptions().setTcpKeepAlive(true))
                .connect(port,host)
                .onItem().invoke(netSocket -> {

                    this.netSocket = netSocket;

                    logger.info(" Entered the net socket stage ");
                    netSocket.handler(buffer -> processMessageRead(buffer))
                            .endHandler(() -> logger.info(" Connection has been severed for {}", id))
                            .exceptionHandler(throwable -> {

                                logger.info(" ############# Exception issued");
                                throwable.printStackTrace();
                            })
                    ;


                })
                .replaceWithVoid();
    }



    public void processMessageRead(Buffer buffer) {

        logger.info(buffer.toString());
        int startOffset = 0;

        byte[] message = buffer.getBytes();

        if(bytes != null){
            message = Bytes.concat(bytes,message);
        }

        while (startOffset < message.length){
            startOffset = processMessage(startOffset,message);
        }
    }


    private int processMessage(int startOffset, byte[] message){

        byte[] lengthBytes =  new byte[2];
        System.arraycopy(message,startOffset,lengthBytes,0,2);
        int messageLength = bytesToInteger(lengthBytes);

        if(startOffset + 2 + messageLength > message.length){
            int length = message.length - startOffset;
            bytes = new byte[message.length - startOffset];
            System.arraycopy(message,startOffset,bytes,0,length);
        } else {
            byte[] restMessage = new byte[messageLength];
            System.arraycopy(message, startOffset + 2,restMessage,0,messageLength);
//            String messageString = new String(restMessage);
//            logger.info(messageString);
            tcpClientService.sendMessageForIncomingTCPProcessing(restMessage,id);
            bytes = null;
        }
        return  startOffset + 2 + messageLength;

    }



    public static int bytesToInteger(byte[] input) {
        BigInteger bigInteger = new BigInteger(1, input);
        return bigInteger.intValue();
    }


    public Uni<Void> closeConnection(){
        return netSocket.end();
    }


    public void writeMessages(byte[] message){

        netSocket.write(Buffer.buffer(message))
                .subscribe().with(unused -> {
                    if(netSocket.writeQueueFull()){
                        netSocket.pause()
                                .drainHandler(() -> netSocket.resume());
                    }
                });

    }

    @Override
    public Uni<Void> asyncStop() {
        logger.info("Deployment Stopped....############ ");
        return super.asyncStop();
    }


    public String getHost(){
        return host;
    }

    public Integer getPort(){

        return port;
    }

    public String getId() {
        return id;
    }
}
