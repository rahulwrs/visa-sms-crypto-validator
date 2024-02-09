package com.zimblesystems.cryptoValidator.startup;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class VaultService {

    private static final Logger logger = LoggerFactory.getLogger(VaultService.class);

    private static final WebClient webClient = WebClient.create(Vertx.vertx(),getClientOptions());



    public static String getKubeAuthToken(Integer port, String host,String role, String jwt) throws ExecutionException, InterruptedException {

        System.out.println("######## Entering the Vault Config");
        CompletableFuture<String> comp = new CompletableFuture<>();

        webClient
                .post(port,host,"/v1/auth/kubernetes/login")
                .sendJsonObject(new JsonObject()
                        .put("jwt", jwt)
                        .put("role",role))
                .onFailure(throwable -> {

                    logger.info("###################Something went wrong " + throwable.getMessage());
                    comp.complete("error");
                })
                .onSuccess(response ->{

                    logger.info(response.bodyAsString().toString());
                    String clientToken = response.bodyAsJsonObject().getJsonObject("auth").getString("client_token");
                    comp.complete(clientToken);

                } )
                ;

        return comp.get();



    }


    public static String getJwt() throws IOException {

//        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("/var/run/secrets/kubernetes.io/serviceaccount/token")){
//
//        }

        try (BufferedReader reader =
                new BufferedReader(new FileReader("/var/run/secrets/kubernetes.io/serviceaccount/token"))) {
            return readAllLines(reader);
        }


    }

    public static String readAllLines(BufferedReader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            content.append(line);
        }

        return content.toString();
    }

    public static Map<String,String> createWebClient(Integer port, String host, String path, String token,boolean secure) throws ExecutionException, InterruptedException {

        logger.info("######## Entering the Vault Config, path, " + path + " host  : " + host + " port  "  + port + " , token  " +  token);
        CompletableFuture<Map<String,String>> comp = new CompletableFuture<>();


         webClient.get(port, host, path)
                .putHeader("X-Vault-Token", token)
                .send()
                .onSuccess(res -> {
                    JsonObject body = res.bodyAsJsonObject();

                    JsonObject jsonObject = body.getJsonObject("data");
                    jsonObject = jsonObject.getJsonObject("data");
                    Map<String, String> propertiesMap = jsonObject.stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().toString()));

                    propertiesMap.forEach((s, s2) -> logger.info(" Property : " + s + " Value :  " + s2));

                    logger.info(
                            "Received response with status code" +
                                    res.statusCode() +
                                    " with body " +
                                    jsonObject);

                    comp.complete(propertiesMap);
                })
                .onFailure(throwable -> {
                    logger.info("###################Something went wrong " + throwable.getMessage());
                    comp.complete(new HashMap<>());
                });

        return comp.get();

    }

    public static WebClientOptions getClientOptions(){

        WebClientOptions webClientOptions = new WebClientOptions();

        if(getSecure()){
            webClientOptions.setSsl(true)
                    .setVerifyHost(false)
            ;

            if(isSkipCertificateValidation()){
                return webClientOptions.setTrustAll(true);
            }

            Optional<String> trustCertificateOptional = getVaultTrustStorePath();
            if(trustCertificateOptional.isPresent()){
                webClientOptions.setPemTrustOptions(new PemTrustOptions().addCertPath(trustCertificateOptional.get()));
            }
        }

        return webClientOptions;

    }



    private static boolean getSecure(){


        String value = System.getenv("CONFIG_VAULT_SECURE");
        if(value == null){
            return false;
        }
        else if(value.equalsIgnoreCase("true")){
            return true;
        } else {
            return false;
        }
    }


    private static boolean isSkipCertificateValidation(){


        String value = System.getenv("QUARKUS_VAULT_TLS_SKIP_VERIFY");
        if(value == null){
            return false;
        }
        else if(value.equalsIgnoreCase("true")){
            return true;
        } else {
            return false;
        }
    }




    private static Optional<String> getVaultTrustStorePath(){

        String value = System.getenv("QUARKUS_VAULT_TLS_CA_CERT");
        if(value == null){
            if(checkIfKube()){
                return Optional.of("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(value);
    }

    private static boolean checkIfKube() {

        String value = System.getenv("CONFIG_DISCOVERY_METHOD");
        if(value == null){
            return false;
        }
        if(value.equalsIgnoreCase("kube")){
            return true;
        }
        return false
        ;

    }
}
