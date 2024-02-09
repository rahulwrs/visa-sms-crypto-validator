package com.zimblesystems.cryptoValidator.service;

import com.zimblesystems.cryptoValidator.model.entity.CryptoProductDef;
import in.nmaloth.payments.keys.HSMKey;
import in.nmaloth.payments.keys.HSMKeyId;
import in.nmaloth.payments.keys.constants.KeyType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Map;

public interface VaultService {

    HSMKeyId createHSMKeyId(int org, int product , KeyType keyType, String zone);

    HSMKey getKey(int org, int product , KeyType keyType, String zone);

    HSMKey getKey(HSMKeyId hsmKeyId);

    void loadKeys(CryptoProductDef cryptoProductDef);

    Multi<String> loadAllKeys();
    Uni<Void> loadAllKeysNonBlocking();

    Multi<Void> loadKeysNonBlocking(CryptoProductDef cryptoProductDef);


    Map<HSMKeyId, HSMKey> getKeysMap();




}
