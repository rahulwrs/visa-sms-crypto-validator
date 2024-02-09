package com.zimblesystems.cryptoValidator.model.entity;

import org.bson.codecs.pojo.annotations.BsonProperty;

public class ValidationServices {


    @BsonProperty("micro_service_name")
    private String microServiceName;
    @BsonProperty("transport_protocol")
    private String transportProtocol;
    @BsonProperty("url_path")
    private String urlPath;
    @BsonProperty("access_token_path")
    private String accessTokenPath;
    @BsonProperty("client_certificate_path")
    private String clientCertificatePath;
    @BsonProperty("server_certificate_path")
    private String serverCertificatePath;
    @BsonProperty("user_path")
    private String userPath;
    @BsonProperty("connect_on_startup")
    private Boolean connectOnStartup;



    public String getMicroServiceName() {
        return microServiceName;
    }

    public void setMicroServiceName(String microServiceName) {
        this.microServiceName = microServiceName;
    }

    public String getTransportProtocol() {
        return transportProtocol;
    }

    public void setTransportProtocol(String transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public String getAccessTokenPath() {
        return accessTokenPath;
    }

    public void setAccessTokenPath(String accessTokenPath) {
        this.accessTokenPath = accessTokenPath;
    }

    public String getClientCertificatePath() {
        return clientCertificatePath;
    }

    public void setClientCertificatePath(String clientCertificatePath) {
        this.clientCertificatePath = clientCertificatePath;
    }

    public String getServerCertificatePath() {
        return serverCertificatePath;
    }

    public void setServerCertificatePath(String serverCertificatePath) {
        this.serverCertificatePath = serverCertificatePath;
    }

    public String getUserPath() {
        return userPath;
    }

    public void setUserPath(String userPath) {
        this.userPath = userPath;
    }

    public Boolean getConnectOnStartup() {
        return connectOnStartup;
    }

    public void setConnectOnStartup(Boolean connectOnStartup) {
        this.connectOnStartup = connectOnStartup;
    }
}
