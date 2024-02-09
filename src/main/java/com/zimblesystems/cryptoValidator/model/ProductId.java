package com.zimblesystems.cryptoValidator.model;

import java.util.Objects;

public class ProductId {

    private int org;
    private int product;

    public ProductId(int org, int product) {
        this.org = org;
        this.product = product;
    }

    public ProductId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductId)) return false;
        ProductId productId = (ProductId) o;
        return getOrg() == productId.getOrg() && getProduct() == productId.getProduct();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrg(), getProduct());
    }

    public int getOrg() {
        return org;
    }

    public void setOrg(int org) {
        this.org = org;
    }

    public int getProduct() {
        return product;
    }

    public void setProduct(int product) {
        this.product = product;
    }
}
