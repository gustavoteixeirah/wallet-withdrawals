package com.teixeirah.withdrawals.domain.events;

import java.util.Collections;
import java.util.Map;

public interface DomainEvent {
    // Provides read-only access, defaults to empty map
    default Map<String, String> getMetadata() {
        return Collections.emptyMap();
    }

    // Returns a copy of this event carrying the given metadata. Default is no-op
    default DomainEvent withMetadata(Map<String, String> metadata) {
        return this;
    }
}
