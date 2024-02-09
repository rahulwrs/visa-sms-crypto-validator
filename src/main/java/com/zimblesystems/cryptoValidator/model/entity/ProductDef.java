
package com.zimblesystems.cryptoValidator.model.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.smallrye.mutiny.Uni;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.List;

@MongoEntity(collection = "product_def",database = "configuration")
public class ProductDef extends ReactivePanacheMongoEntity {

    @BsonProperty("org")
    private Integer org;
    @BsonProperty("product")
    private Integer product;
    @BsonProperty("validation_services")
    private List<ValidationServices> validationServicesList;


    public static Uni<ProductDef> findSystem(){
        return find("org= ?1 and product= ?2",0,0).firstResult()
               ;
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


    public List<ValidationServices> getValidationServicesList() {
        return validationServicesList;
    }

    public void setValidationServicesList(List<ValidationServices> validationServicesList) {
        this.validationServicesList = validationServicesList;
    }
}
