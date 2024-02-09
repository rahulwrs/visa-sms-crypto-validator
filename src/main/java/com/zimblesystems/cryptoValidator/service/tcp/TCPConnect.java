package com.zimblesystems.cryptoValidator.service.tcp;

import io.smallrye.config.ConfigMapping;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "discovery.hsm.config")
public interface TCPConnect {

    List<TCPInfo> infoList();

    interface TCPInfo {

        String host();
        Integer port();
        Optional<Boolean> onStartup();


    }
}
