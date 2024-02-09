package com.zimblesystems.cryptoValidator.serviceEvents.model;

public enum ServiceAction {

    SETUP_CLIENT,
    PROCESS_REQUEST,
    REMOVE_CLIENT,
    SERVICE_DISCOVERY,
    SERVICE_DISCOVERY_ALL,
    CONNECT_TCP,
    REMOVE_TCP,
    RECONNECT_TCP,
    CHECK_STATUS_AVAILABLE,
    ADD_WATCHER,
    ADD_WATCHER_SERVICE,
    DIAGNOSTIC_HSM

    ;
}
