package com.zimblesystems.cryptoValidator.model.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.smallrye.mutiny.Uni;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.Optional;

@MongoEntity(collection = "crypto_product_def",database = "configuration")
public class CryptoProductDef extends ReactivePanacheMongoEntity {

    @BsonProperty("org")
    private Integer org;
    @BsonProperty("product")
    private Integer product;
    @BsonProperty("key_path")
    private String keyPath;
    @BsonProperty("pin_length")
    private Integer pinLength;
    @BsonProperty("decimalisation_table")
    private String decimalisationTable;


    public static Uni<Optional<CryptoProductDef>> findByOrgAndProduct(Integer org, Integer product){
        return find("org= ?1 and product =?2",org,product).firstResultOptional();
    }

    public Integer getOrg() {
        return org;
    }

    public void setOrg(Integer org) {
        this.org = org;
    }

    public Integer getProduct() {
        return product;
    }

    public void setProduct(Integer product) {
        this.product = product;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    public Integer getPinLength() {
        return pinLength;
    }

    public void setPinLength(Integer pinLength) {
        this.pinLength = pinLength;
    }

    public String getDecimalisationTable() {
        return decimalisationTable;
    }

    public void setDecimalisationTable(String decimalisationTable) {
        this.decimalisationTable = decimalisationTable;
    }
}
