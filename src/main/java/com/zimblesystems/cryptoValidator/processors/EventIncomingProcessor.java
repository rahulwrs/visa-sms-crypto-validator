package com.zimblesystems.cryptoValidator.processors;


import com.zimblesystems.cryptoValidator.listeners.MessageListener;

public interface EventIncomingProcessor<T> {

    void registerFluxListeners(MessageListener<T> messageListener);
    void processMessage(T message);


}
