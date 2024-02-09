package com.zimblesystems.cryptoValidator.model.hsm;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;


public class ConnectDTO {

    @JsonProperty("host")
    @NotNull
    private String host;
    @JsonProperty("port")
    @NotNull
    private Integer port;
    @JsonProperty("id")
    private String id;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
