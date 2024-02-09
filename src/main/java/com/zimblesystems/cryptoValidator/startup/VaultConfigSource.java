package com.zimblesystems.cryptoValidator.startup;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class VaultConfigSource implements ConfigSource {

    private static Logger logger = LoggerFactory.getLogger(VaultConfigSource.class);
    private Map<String, String> configuration = new HashMap<>();


    public VaultConfigSource() throws InterruptedException, IOException {

        try {
            updateProperties();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Map<String, String> getProperties() {

        return this.configuration;
    }

    @Override
    public Set<String> getPropertyNames() {

//        try {
//            updateProperties();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return configuration.keySet();
    }

    @Override
    public int getOrdinal() {
        return 998;
    }

    @Override
    public String getValue(String s) {
//
//        try {
//            updateProperties();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return configuration.get(s);

    }

    @Override
    public String getName() {
        return VaultConfigSource.class.getSimpleName();
    }

    private String getConfigPath() {


//        String path = ConfigProvider.getConfig().getValue("CONFIG_VAULT_PATH", String.class);
        Optional<String> pathOptional = getEnvironmentValue("CONFIG_VAULT_PATH");

        if (pathOptional.isEmpty()) {
            return "/v1/secret/data/config/info";
        } else {
            return pathOptional.get();
        }

    }


    private String getConfigRole() {

//        String role = ConfigProvider.getConfig().getValue("CONFIG_VAULT_ROLE",String.class);
        Optional<String> roleOptional = getEnvironmentValue("CONFIG_VAULT_ROLE");

        if (roleOptional.isEmpty()) {
            return "config";
        } else {
            return roleOptional.get();
        }

    }

    private Optional<String> getAccessToken() {

//        String jwt = ConfigProvider.getConfig().getValue("CONFIG_VAULT_ACCESS_TOKEN", String.class);
        return getEnvironmentValue("QUARKUS_VAULT_AUTHENTICATION_CLIENT_TOKEN");
//
//        if (jwt == null) {
//            return Optional.empty();
//        } else {
//            return Optional.of(jwt);
//        }

    }

    private Optional<String> getVaultHost() {


//        String host = ConfigProvider.getConfig().getValue("CONFIG_VAULT_HOST", String.class);
        return getEnvironmentValue("CONFIG_VAULT_HOST");

//        if (host == null) {
//            return Optional.empty();
//        } else {
//            return Optional.of(host);
//        }

    }

    private boolean getSecure(){

        Optional<String> secureOptional = getEnvironmentValue("CONFIG_VAULT_SECURE");

        if (secureOptional.isEmpty()) {
            return false;
        } else if(secureOptional.get().equalsIgnoreCase("true")){
            return true;
        } else {
            return false;
        }
    }


    private Integer getVaultPort() {

//        String port = ConfigProvider.getConfig().getValue("CONFIG_VAULT_PORT", String.class);
        Optional<String> portOptional = getEnvironmentValue("CONFIG_VAULT_PORT");

        if (portOptional.isEmpty()) {
            return 8200;
        } else {
            return Integer.parseInt(portOptional.get());
        }

    }

    private Boolean checkIfVaultPropertiesActive() {

//        String vaultActive = ConfigProvider.getConfig().getValue("CONFIG_VAULT_ACTIVE", String.class);
        Optional<String> vaultActiveOptional = getEnvironmentValue("CONFIG_VAULT_ACTIVE");

        if (vaultActiveOptional.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(vaultActiveOptional.get());
    }

    private void updateProperties() throws ExecutionException, InterruptedException, IOException {


//        logger.info(" ############## Entered");


        Map<String, String> map = System.getenv().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue))

                ;

        System.getenv().entrySet().stream()
                .forEach(entry -> logger.info(" key :  " + entry.getKey() +   " , value : " + entry.getValue()));


//        ConfigProvider.getConfig().getPropertyNames().forEach(s -> logger.info("########  property name is : {}", s));

        if (checkIfVaultPropertiesActive()) {
//            logger.info("################Entereing Config Source Info");


//            String host = getVaultHost().orElseThrow(() -> new RuntimeException(" Vault Host not mentioned "));

            String urlString = getVaultUrl().orElseThrow(() -> new RuntimeException(" Vault url not mentioned "));

            URL url = new URL(urlString);

//            Integer port = getVaultPort();

            String configPath = getConfigPath();

            Optional<String> accessTokenOptional = getAccessToken();

            if (accessTokenOptional.isPresent()) {
                String accessToken = accessTokenOptional.get();
                configuration = VaultService.createWebClient(url.getPort(), url.getHost(), configPath, accessToken,getSecure())
                ;
//                String url = "http://" + host + ":" + port;
//
//                if(getSecure()){
//                    url = "https://" + host + ":" + port;
//                }
//                logger.info("Vault URL : {}", url );
//                configuration.put("quarkus.vault.authentication.client-token",accessToken);
//                configuration.put("quarkus.vault.url",url);


            } else {

                String jwt = VaultService.getJwt();

                logger.info(jwt);

                String accessToken = VaultService.getKubeAuthToken(url.getPort(), url.getHost(), getConfigRole(), jwt);

                if (accessToken == null) {
                    logger.info(" Recieved Error ....");
                } else {
                    configuration = VaultService.createWebClient(url.getPort(), url.getHost(), configPath,
                            accessToken,getSecure())
                    ;
//                    String url = "http://" + host + ":" + port;
//
//                    if(getSecure()){
//                        url = "https://" + host + ":" + port;
//                    }
//                    logger.info("Vault URL : {}", url );
//                    configuration.put("quarkus.vault.authentication.client-token",accessToken);
//                    configuration.put("quarkus.vault.url",url);


                }

            }

        }

    }



    private Optional<String> getVaultUrl() {


//        String host = ConfigProvider.getConfig().getValue("CONFIG_VAULT_HOST", String.class);
        return getEnvironmentValue("QUARKUS_VAULT_URL");

//        if (hostOptional.isEmpty()) {
//            return Optional.empty();
//        } else {
//            return hostOptional;
//        }

    }

    public Optional<String> getEnvironmentValue(String envVariable){

        String value = System.getenv(envVariable);
        if(value == null){
            return Optional.empty();
        }
        return Optional.of(value.replaceAll("(\\r|\\n|\\t)", ""));
    }


}
