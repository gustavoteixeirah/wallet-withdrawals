package com.teixeirah.withdrawals.infrastructure.secondary.messaging;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class DomainEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(List<DomainEvent> events) {
        log.info("Publishing {} domain events", events.size());

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // If there's an active transaction, publish events after commit
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishEvents(events);
                }
            });
        } else {
            publishEvents(events);
        }
    }

    private void publishEvents(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            log.info("Publishing event: {}", event);
            applicationEventPublisher.publishEvent(event);
        }
        log.info("All domain events published successfully");
    }
}