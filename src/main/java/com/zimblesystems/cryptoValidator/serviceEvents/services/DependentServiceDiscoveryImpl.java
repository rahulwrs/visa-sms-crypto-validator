package com.zimblesystems.cryptoValidator.serviceEvents.services;

import com.zimblesystems.cryptoValidator.serviceEvents.config.ServiceDiscoveryConfig;
import com.zimblesystems.cryptoValidator.serviceEvents.model.discovery.ServerInfo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class DependentServiceDiscoveryImpl implements DependentServiceDiscovery{

    private final static Logger logger = LoggerFactory.getLogger(DependentServiceDiscoveryImpl.class);

    @Inject
    ServiceDiscoveryConfig serviceDiscoveryConfig;


//R    @Inject
//R    ServiceDiscoveryKube serviceDiscoveryKube;


    @Override
    public Uni<List<ServerInfo>> discoverServices(String serviceName) {

        if(serviceDiscoveryConfig.method().equalsIgnoreCase("static")) {

            if(serviceDiscoveryConfig.services() == null){

                return Uni.createFrom().item(new ArrayList<>());

//                throw new RuntimeException("No Service Info mentioned for connections");
            }

            if(serviceDiscoveryConfig.services().size() == 0){
                return Uni.createFrom().item(new ArrayList<>());
            }


            return Uni.createFrom().item(serviceDiscoveryConfig.services()
                    .stream().filter(service -> serviceName.equalsIgnoreCase(service.grpc()))
                    .flatMap(service -> processServiceInfo(service).stream())
                    .collect(Collectors.toList()))
                    ;


        }  else if(serviceDiscoveryConfig.method().equalsIgnoreCase("kube")){

//R            return serviceDiscoveryKube.fetchServicesUni(serviceName);
            return null;    //R
        }

        else {throw new RuntimeException("Discovery Method " + serviceDiscoveryConfig.method() + " Not Supported");}
    }

    @Override
    public Uni<List<ServerInfo>> discoverAllDependentServices() {

        logger.info(" ############### Entered Discovery Service All ");
        logger.info("############ Service Discovery Option : {}", serviceDiscoveryConfig.method());

        if(serviceDiscoveryConfig.method().equalsIgnoreCase("static")){

            if(serviceDiscoveryConfig.services() == null){
                return Uni.createFrom().item(new ArrayList<>());

//                throw new RuntimeException("No Service Info mentioned for connections");
            }

            if(serviceDiscoveryConfig.services().size() == 0){
                return Uni.createFrom().item(new ArrayList<>());

//                throw new RuntimeException("No Service Info mentioned for connections");
            }

            return Uni.createFrom().item(serviceDiscoveryConfig.services()
                    .stream()
                    .flatMap(service -> processServiceInfo(service).stream())
                    .collect(Collectors.toList()))
                    ;



        } else if (serviceDiscoveryConfig.method().equalsIgnoreCase("kube")){
//R            return serviceDiscoveryKube.discoverAllDependentServicesUni();
            return null; //R
        }
        else {
            throw new RuntimeException("Discovery Method " + serviceDiscoveryConfig.method() + " Not Supported");
        }
    }

    private List<ServerInfo> processServiceInfo(ServiceDiscoveryConfig.Service service) {


        if(service.instances() == null ){
//            return new ArrayList<>();
            throw new RuntimeException(" Instance Information is required for static service discovery");
        }

        return service.instances()
                .stream().map(instance ->
                        ServerInfo.builder()
                        .serviceName(service.grpc())
                        .serviceInstance(instance.instance())
                        .host(instance.host())
                        .port(instance.port())
                        .build()
                )
                .collect(Collectors.toList())

        ;
    }

//    @Override
//    public void addWatchers() {
//
//        if(!serviceDiscoveryConfig.method().equalsIgnoreCase("kube")){
//            return;
//        }
//
//        if(serviceDiscoveryConfig.services() != null && serviceDiscoveryConfig.services().size() > 0){
//            List<ServiceDiscoveryConfig.Service> servicesLDiscoveryList = serviceDiscoveryConfig.services();
//            servicesLDiscoveryList.stream()
//                    .forEach(service -> serviceDiscoveryKube.createWatcher(service.grpc()))
//
//            ;
//        }
//
//    }

//    @Override
//    public void addWatchers(String serviceName) {
//        serviceDiscoveryKube.createWatcher(serviceName);
//    }


}
