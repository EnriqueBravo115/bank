package dev.enrique.bank.commons.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.enrique.bank.commons.dto.request.BaseRequest;

@Component
public class TransactionFactoryProvider {
    private final Map<Class<? extends BaseRequest>, TransactionFactory<? extends BaseRequest>> factories;

    @Autowired
    public TransactionFactoryProvider(List<TransactionFactory<? extends BaseRequest>> factoryList) {
        Map<Class<? extends BaseRequest>, TransactionFactory<? extends BaseRequest>> tempMap = new HashMap<>();
        for (TransactionFactory<? extends BaseRequest> factory : factoryList) {
            tempMap.put(factory.getRequestType(), factory);
        }
        this.factories = Collections.unmodifiableMap(tempMap);
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseRequest> TransactionFactory<T> getFactory(Class<T> requestType) {
        TransactionFactory<T> factory = (TransactionFactory<T>) factories.get(requestType);
        if (factory == null) {
            throw new IllegalArgumentException("No factory registered for " + requestType);
        }
        return factory;
    }
}
