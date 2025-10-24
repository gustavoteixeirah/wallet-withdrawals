package com.teixeirah.withdrawals.domain.events;

import java.util.List;

public interface DomainEventPublisherPort {

    void publish(List<DomainEvent> events);

}
