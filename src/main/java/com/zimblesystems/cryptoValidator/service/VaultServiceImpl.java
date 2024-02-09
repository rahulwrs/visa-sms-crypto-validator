package com.zimblesystems.cryptoValidator.service;

import com.zimblesystems.cryptoValidator.model.entity.CryptoProductDef;

import in.nmaloth.payments.keys.HSMKey;
import in.nmaloth.payments.keys.HSMKeyId;
import in.nmaloth.payments.keys.constants.KeyType;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vault.VaultKVSecretEngine;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class VaultServiceImpl implements VaultService {


    private final Map<HSMKeyId, HSMKey> hsmKeyValueMap = new HashMap<>();
    private final VaultKVSecretEngine kvSecretEngine;
    private static final Logger logger = LoggerFactory.getLogger(VaultServiceImpl.class);

    private final CryptoService cryptoService;

    public VaultServiceImpl(VaultKVSecretEngine kvSecretEngine, CryptoService cryptoService) {
        this.kvSecretEngine = kvSecretEngine;
        this.cryptoService = cryptoService;
    }


    public void startup(@Observes StartupEvent startupEvent){

        CryptoProductDef.<CryptoProductDef>listAll().await().indefinitely()
                .forEach(cryptoProductDef -> loadKeys(cryptoProductDef));

    }

    @Override
    public void loadKeys(CryptoProductDef cryptoProductDef) {


        if (cryptoProductDef.getKeyPath() != null) {
            loadKeys(cryptoProductDef.getKeyPath(), cryptoProductDef.getOrg(), cryptoProductDef.getProduct());
        }

    }

    @Override
    public Multi<String> loadAllKeys(){

        return cryptoService.loadAllProducts()
                .onItem().invoke(cryptoProductDef -> loadKeys(cryptoProductDef.getKeyPath(),cryptoProductDef.getOrg(),cryptoProductDef.getProduct()))
                .onItem().transform(cryptoProductDef -> cryptoProductDef.getKeyPath())
                ;
    }


    @Override
    public Uni<Void> loadAllKeysNonBlocking(){

        return cryptoService.loadAllProducts()
                .onItem().transformToMultiAndConcatenate(this::loadKeysNonBlocking)
                .collect().asList()
                .replaceWithVoid();
    }

    @Override
    public Multi<Void> loadKeysNonBlocking(CryptoProductDef cryptoProductDef) {

        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> loadKeys(cryptoProductDef));

        return Uni.createFrom().future(completableFuture)
                .toMulti()
                ;
    }

    @Override
    public Map<HSMKeyId, HSMKey> getKeysMap() {
        return hsmKeyValueMap;
    }

    private void loadKeys(String keyPath, int org, int product) {

        try {

            logger.info( "####### Path info : {}",keyPath );

            Map<String, String> keyMap = kvSecretEngine.readSecret(keyPath);
            keyMap.forEach((keyId, key) -> hsmKeyValueMap.put(generateKeyId(keyId, org, product), generateKey(key)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    private HSMKeyId generateKeyId(String keyIdString, int org, int product){

        String[] keyIdArray = keyIdString.split(";");
        HSMKeyId keyId = new HSMKeyId();
        keyId.setOrg(org);
        keyId.setProduct(product);
        keyId.setKeyType(KeyType.valueOf(keyIdArray[0]));
        if(keyIdArray.length >  1){
            keyId.setZone(keyIdArray[1]);
        } else {
            keyId.setZone("00");
        }

        return keyId;
    }

    private HSMKey generateKey(String keyString){

        HSMKey hsmKey = new HSMKey();
        String[] keyArray = keyString.split(";");

        hsmKey.setKeyScheme(keyArray[0]);
        hsmKey.setKey(keyArray[1]);
        hsmKey.setKcv(keyArray[2]);
        if(keyArray.length > 3){
            hsmKey.setRndi(keyArray[3]);
        }
        return hsmKey;
    }



    @Override
    public HSMKey getKey(HSMKeyId hsmKeyId) {
        return hsmKeyValueMap.get(hsmKeyId);
    }

    @Override
    public HSMKey getKey(int org, int product, KeyType keyType, String zone) {

        return getKey(createHSMKeyId(org,product,keyType,zone));
    }


    @Override
    public HSMKeyId createHSMKeyId(int org, int product, KeyType keyType, String zone) {

        HSMKeyId hsmKeyId = new HSMKeyId();
        hsmKeyId.setOrg(org);
        hsmKeyId.setProduct(product);
        hsmKeyId.setKeyType(keyType);
        hsmKeyId.setZone(zone);
        return hsmKeyId;
    }



}