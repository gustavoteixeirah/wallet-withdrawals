package com.teixeirah.withdrawals.infrastructure.secondary.messaging;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(List<DomainEvent> events) {
        log.info("Publishing {} domain events", events.size());

        for (DomainEvent event : events) {
            log.info("Publishing event: {}", event);
            applicationEventPublisher.publishEvent(event);
        }

        log.info("All domain events published successfully");
    }
}