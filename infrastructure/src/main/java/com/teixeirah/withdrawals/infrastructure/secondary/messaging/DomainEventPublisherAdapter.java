package com.teixeirah.withdrawals.infrastructure.secondary.messaging;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component("defaultEventPublisher")
class DomainEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @WithSpan(value = "Publishing domain events after transaction commit")
    public void publish(@SpanAttribute("events") List<DomainEvent> events) {
        log.atInfo()
           .addKeyValue("eventsCount", events.size())
           .log("publishing_domain_events_registered_after_commit");

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishEvents(events);
            }
        });

    }

    private void publishEvents(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            log.atInfo()
               .addKeyValue("eventType", event.getClass().getName())
               .log("publishing_domain_event");
            applicationEventPublisher.publishEvent(event);
        }
        log.atInfo()
           .addKeyValue("eventsCount", events.size())
           .log("domain_events_published");
    }
}