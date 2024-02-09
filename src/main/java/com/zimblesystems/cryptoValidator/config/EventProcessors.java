package com.zimblesystems.cryptoValidator.config;

import com.zimblesystems.cryptoValidator.model.proto.aggregator.ValidationResponseSummary;
import com.zimblesystems.cryptoValidator.model.proto.crypto.CryptoResponse;
import com.zimblesystems.cryptoValidator.processors.EventOutgoingProcessor;
import com.zimblesystems.cryptoValidator.processors.EventOutgoingProcessorImpl;
import com.zimblesystems.cryptoValidator.serviceEvents.model.Connections;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EventProcessors {


    public final String INSTANCE = UUID.randomUUID().toString().replace("-","");

    public final List<Connections> connections = new ArrayList<>();

    public final EventOutgoingProcessor<ValidationResponseSummary> aggregatorProcessor =  new EventOutgoingProcessorImpl<>();

    public final EventOutgoingProcessor<CryptoResponse> cryptoResponseProcessor =  new EventOutgoingProcessorImpl<>();




}
