package com.zimblesystems.cryptoValidator.service.tcp;



import com.zimblesystems.cryptoValidator.model.hsm.ConnectDTO;
import com.zimblesystems.cryptoValidator.verticles.TCPClientVerticle;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface TCPClientService {

    void createClient(String host, Integer port, int reconnectCount);

    Uni<String> createClient(ConnectDTO connectDTO);

    void deleteTCPVerticle(TCPClientVerticle tcpClientVerticle);

    void sendMessage(byte[] message,String id);

    void sendMessage(byte[] message);

    Uni<Void> closeConnections(String id);

    List<TCPClientVerticle> getAllTCPVerticles();

    List<ConnectDTO> connectionsOfHSM();

    void sendMessageForIncomingTCPProcessing(byte[] message, String channelId);



}
