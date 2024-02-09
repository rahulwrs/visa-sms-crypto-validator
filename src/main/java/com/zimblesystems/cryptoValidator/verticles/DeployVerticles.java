package com.zimblesystems.cryptoValidator.verticles;

import com.zimblesystems.cryptoValidator.config.EventBusNames;
import com.zimblesystems.cryptoValidator.config.ServiceNames;
import com.zimblesystems.cryptoValidator.model.entity.ProductDef;
import com.zimblesystems.cryptoValidator.model.entity.ValidationServices;
import com.zimblesystems.cryptoValidator.model.proto.crypto.CryptoValidator;
import com.zimblesystems.cryptoValidator.service.tcp.TCPConnect;
import com.zimblesystems.cryptoValidator.serviceEvents.model.ServiceAction;
import com.zimblesystems.cryptoValidator.serviceEvents.model.ServiceEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.LocalEventBusCodec;
import io.vertx.core.DeploymentOptions;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@ApplicationScoped
public class DeployVerticles {


    @Inject
    Vertx vertx;
    @Inject
    io.vertx.core.Vertx vertx1;

    @ConfigProperty(name = "request.deploy.instances")
    Optional<Integer> requestVerticleDeployInstances;

    @Inject
    TCPConnect tcpConnect;


    private static final Logger logger = LoggerFactory.getLogger(DeployVerticles.class);


    public void deployVerticles(@Observes StartupEvent startupEvent,
                                Instance<CryptoRequestVerticle> requestVerticles,
                                ServiceEventVerticle serviceEventVerticle
    ) {

        vertx1.eventBus().registerDefaultCodec(CryptoValidator.class, new LocalEventBusCodec<>());
        vertx1.eventBus().registerDefaultCodec(ServiceEvent.class, new LocalEventBusCodec<>());

        vertx.deployVerticle(requestVerticles::get, new DeploymentOptions().setInstances(requestVerticleDeployInstances.orElse(2)))
                .onFailure().invoke(throwable -> {
                    throwable.printStackTrace();
                    throw new RuntimeException(throwable.getMessage());
                })
                .await().indefinitely()
        ;
        logger.info("############### deployed Request Verticles");


        vertx.deployVerticle(serviceEventVerticle)
                .onFailure().invoke(throwable -> {
                    throwable.printStackTrace();
                    throw new RuntimeException(throwable.getMessage());
                })
                .await().indefinitely()
        ;
        logger.info(" ########## deployed Service Event Verticle");


        vertx.eventBus().send(EventBusNames.SERVICE_EVENTS,
                ServiceEvent.builder().serviceAction(ServiceAction.SERVICE_DISCOVERY_ALL).build());

//R        vertx.eventBus().send(EventBusNames.SERVICE_EVENTS,
//R                ServiceEvent.builder().serviceAction(ServiceAction.ADD_WATCHER).build());

// Connect to HSM (TCP Connection )

        if (tcpConnect.infoList().size() > 0) {

            tcpConnect.infoList()
                    .forEach(serverInfo -> {
                        if (serverInfo.onStartup().isPresent() && serverInfo.onStartup().get()) {
                            vertx.eventBus().send(EventBusNames.SERVICE_EVENTS,
                                    ServiceEvent.builder().serviceName(ServiceNames.HSM_TCP)
                                            .host(serverInfo.host())
                                            .port(serverInfo.port())
                                            .attempts(0)
                                            .serviceAction(ServiceAction.CONNECT_TCP)
                                            .build()
                            );
                        }
                    });
        } else {

            ProductDef productDef = ProductDef.findSystem().await().indefinitely();
            ValidationServices validationServices = productDef.getValidationServicesList()
                    .stream()
                    .filter(validationService -> validationService.getMicroServiceName().equals(ServiceNames.HSM_TCP))
                    .findFirst().get();

            String[] ipStringArray = validationServices.getUrlPath().split(",");

            for (String ipInfo : ipStringArray) {

                String ipHost = ipInfo.split(":")[0];
                Integer port = Integer.parseInt(ipInfo.split(":")[1]);

                if (validationServices.getConnectOnStartup() != null && validationServices.getConnectOnStartup()) {
                    vertx.eventBus().send(EventBusNames.SERVICE_EVENTS, ServiceEvent.builder()
                            .serviceName(ServiceNames.HSM_TCP)
                            .serviceAction(ServiceAction.CONNECT_TCP)
                            .host(ipHost)
                            .port(port)
                            .build())
                    ;
                }
            }
        }


    }

}
