package com.zimblesystems.cryptoValidator.verticles;

import com.zimblesystems.cryptoValidator.config.EventBusNames;
import com.zimblesystems.cryptoValidator.model.CryptoData;
import com.zimblesystems.cryptoValidator.model.proto.crypto.CryptoValidator;
import com.zimblesystems.cryptoValidator.service.CryptoService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class CryptoRequestVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(CryptoRequestVerticle.class);
    @Inject
    CryptoService cryptoService;


    @Override
    public Uni<Void> asyncStart() {
        return vertx.eventBus().<CryptoValidator>consumer(EventBusNames.CRYPTO_BUS)
                .handler(cryptoValidatorMessage -> {

                    CryptoData cryptoData = cryptoService.convertProto(cryptoValidatorMessage.body());

                    if(cryptoService.validateCryptoData(cryptoData)){

                        logger.info("########### Crypto validation required : true");

                        cryptoService.sendMessageToHSM(cryptoData);
                        logger.info("########### Crypto Messages Sent ");

                    }
                }).exceptionHandler(throwable -> throwable.printStackTrace())
                .completionHandler()
                ;
    }
}
