package com.teixeirah.withdrawals.domain.events;

import java.util.HashMap;
import java.util.Map;

public interface DomainEvent {

    Map<String, String> metadata = new HashMap<>();

    default Map<String, String> getMetadata() {
        return metadata;
    }

    default void setMetadata(String key, String value) {
        metadata.put(key, value);
    }
}
