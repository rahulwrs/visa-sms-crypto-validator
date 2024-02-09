package com.zimblesystems.cryptoValidator.serviceEvents.services;

import com.zimblesystems.cryptoValidator.serviceEvents.model.discovery.ServerInfo;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface DependentServiceDiscovery {

    Uni<List<ServerInfo>> discoverServices(String serviceName);
    Uni<List<ServerInfo>> discoverAllDependentServices();
//    void addWatchers();
//    void addWatchers(String serviceName);


}
