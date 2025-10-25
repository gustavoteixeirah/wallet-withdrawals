package com.teixeirah.withdrawals.application.usecase;

import java.util.HashMap;
import java.util.Map;

public class WrapperContext {
    private final Map<String, Object> attributes = new HashMap<>();

    public WrapperContext with(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public <T> T get(String key, Class<T> type) {
        return type.cast(attributes.get(key));
    }
}