package com.zimblesystems.cryptoValidator.startup;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DiscoveryConfig implements ConfigSource {

    private static Logger logger = LoggerFactory.getLogger(DiscoveryConfig.class);
    private Map<String, String> configuration = new HashMap<>();

    public DiscoveryConfig() throws IOException, ExecutionException, InterruptedException {
        updateProperties();
    }

    @Override
    public Set<String> getPropertyNames() {
        return configuration.keySet();
    }

    @Override
    public String getValue(String s) {
        return configuration.get(s);
    }

    @Override
    public Map<String, String> getProperties() {
        return this.configuration;
    }

    @Override
    public int getOrdinal() {
        return 275;
    }

    @Override
    public String getName() {
        return DiscoveryConfig.class.getSimpleName();
    }


    private Integer getConfigDiscoveryCount() {

//        String path = ConfigProvider.getConfig().getValue("CONFIG_VAULT_PATH", String.class);
        Optional<String> countStringOptional = getEnvironmentValue("CONFIG_KUBE_SERVICE_COUNT");



        if (countStringOptional.isEmpty()) {
            return 0;
        } else {
            return Integer.parseInt(countStringOptional.get());
        }

    }

    private Optional<String> getConfig(String config){

        return getEnvironmentValue(config);

    }

    private void populateDiscoveryService(Integer count) {

        Optional<String> configDiscoveryMethod = getConfig("CONFIG_DISCOVERY_METHOD");
        if(configDiscoveryMethod.isPresent()){
            configuration.put("discovery.method",configDiscoveryMethod.get());
        }

        Optional<String> configTestMode = getConfig("CONFIG_TEST_MODE");
        if(configTestMode.isPresent()){
            configuration.put("quarkus.test.mode",configTestMode.get());
        }


        for(int i = 0; i < count; i ++){


            String configServiceName = "CONFIG_KUBE_SERVICE_" + i;
            Optional<String> configServiceOptional = getConfig(configServiceName);
            if(configServiceOptional.isEmpty()){
                throw new RuntimeException("No Valid Service ");
            }
            configuration.put("discovery.services[" + i + "].name",configServiceOptional.get());

            configServiceName = "CONFIG_KUBE_SERVICE_GRPC_" + i;
            Optional<String> configServiceGrpcOptional = getConfig(configServiceName);
            if(configServiceGrpcOptional.isEmpty()){
                throw new RuntimeException("No Valid GRPC Service ");
            }
            configuration.put("discovery.services[" + i + "].grpc",configServiceGrpcOptional.get());

            configServiceName = "CONFIG_KUBE_SERVICE_LABEL_KEY_" + i;
            Optional<String> configServiceLabelKeyOptional = getConfig(configServiceName);
            if(configServiceLabelKeyOptional.isEmpty()){
                throw new RuntimeException("No Valid Service Label ");
            }
            configuration.put("discovery.services[" + i + "].label.key",configServiceLabelKeyOptional.get());

            configServiceName = "CONFIG_KUBE_SERVICE_LABEL_VALUE_" + i;
            Optional<String> configServiceLabelValueOptional = getConfig(configServiceName);
            if(configServiceLabelValueOptional.isEmpty()){
                throw new RuntimeException("No Valid Service Label Value ");
            }
            configuration.put("discovery.services[" + i + "].label.value",configServiceLabelValueOptional.get());

        }


    }



    private void updateProperties() throws ExecutionException, InterruptedException, IOException {


//        logger.info(" ############## Entered");


        Map<String, String> map = System.getenv().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue))

                ;

        System.getenv().entrySet().stream()
                .forEach(entry -> logger.info(" key :  " + entry.getKey() +   " , value : " + entry.getValue()));


        Integer count = getConfigDiscoveryCount();

        populateDiscoveryService(count);

        populateDiscoveryServiceHSM(getConfigDiscoveryHSMCount());



//        ConfigProvider.getConfig().getPropertyNames().forEach(s -> logger.info("########  property name is : {}", s));

    }

    public Optional<String> getEnvironmentValue(String envVariable){

        String value = System.getenv(envVariable);
        if(value == null){
            return Optional.empty();
        }
        return Optional.of(value.replaceAll("(\\r|\\n|\\t)", ""));
    }

    private void populateDiscoveryServiceHSM(Integer count) throws MalformedURLException {


        for(int i = 0; i < count; i ++){


            Optional<String> hsmURLOptional = getConfig("DISCOVER_HSM_URL_" + i);

            Optional<String> onStartupOptional = getConfig("DISCOVER_HSM_STARTUP_" + i);

            if(hsmURLOptional.isPresent()){

                URL url = new URL(hsmURLOptional.get());

                logger.info("################ URL Host and port are as ..... {},  Port: ",url.getHost(),url.getPort());
                configuration.put("discovery.hsm.config.info-list[" + i + "].host",url.getHost());
                configuration.put("discovery.hsm.config.info-list[" + i + "].port",Integer.valueOf(url.getPort()).toString());
                if(onStartupOptional.isPresent() && Boolean.parseBoolean(onStartupOptional.get())){
                    configuration.put("discovery.hsm.config.info-list[" + i + "].on-startup","true");
                }
            }


        }


    }

    private Integer getConfigDiscoveryHSMCount() {

//        String path = ConfigProvider.getConfig().getValue("CONFIG_VAULT_PATH", String.class);
        Optional<String> countStringOptional = getEnvironmentValue("CONFIG_KUBE_HSM_COUNT");



        if (countStringOptional.isEmpty()) {
            return 0;
        } else {
            return Integer.parseInt(countStringOptional.get());
        }

    }

}
