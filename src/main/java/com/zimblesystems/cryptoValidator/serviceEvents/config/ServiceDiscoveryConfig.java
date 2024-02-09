package com.zimblesystems.cryptoValidator.serviceEvents.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "discovery")
public interface ServiceDiscoveryConfig {

    @WithName("method")
    String method();
    List<Service> services();

    interface Service {

        String name();
        String grpc();
        Optional<Label> label();
        List<Instance> instances();

        interface Label{
            String key();
            String value();
        }

        interface Instance {
            String instance();
            String host();
            Integer port();
        }

    }

}
