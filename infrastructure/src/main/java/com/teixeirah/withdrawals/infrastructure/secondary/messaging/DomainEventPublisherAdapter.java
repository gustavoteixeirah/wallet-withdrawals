package com.teixeirah.withdrawals.infrastructure.secondary.messaging;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
class DomainEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;
    // Inject the OTel propagator bean (Spring Boot auto-configures one)
    private final TextMapPropagator textMapPropagator;

    // Helper for TextMapPropagator.inject
    private final TextMapSetter<Map<String, String>> mapSetter =
            (carrier, key, value) -> carrier.put(key, value);

    @Override
    @WithSpan(value = "publish_domain_events_after_transaction_commit")
    public void publish(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        log.atDebug() // Use Debug level for registration, Info for actual publishing
                .addKeyValue("eventsCount", events.size())
                .log("registering_domain_events_for_publishing_after_commit");

        // Capture context *before* registering, used only to wrap the afterCommit logic
        final Context capturedContextForCallback = Context.current();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Wrap the callback logic in the context captured *before* registration
                // This ensures the @WithSpan is still the parent if the callback thread changes
                try (Scope scope = capturedContextForCallback.makeCurrent()) {
                    log.atInfo()
                            .addKeyValue("eventsCount", events.size())
                            .log("publishing_domain_events_registered_after_commit");

                    // Get the *truly* current context *INSIDE* the afterCommit scope
                    // This context definitely contains the active span.
                    Context contextToInject = Context.current();

                    // Logging for verification
                    Span currentSpan = Span.fromContext(contextToInject);
                    log.atDebug()
                            .addKeyValue("traceId", currentSpan.getSpanContext().getTraceId())
                            .addKeyValue("spanId", currentSpan.getSpanContext().getSpanId())
                            .log("Injecting context into event metadata");

                    publishEventsWithContext(events, contextToInject);
                } catch (Exception e) {
                    log.error("Failed to publish domain events after commit", e);
                    Span currentSpan = Span.current(); // Get span active during the exception
                    if (currentSpan != null && currentSpan.isRecording()) {
                        currentSpan.recordException(e);
                        currentSpan.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Failed to publish events");
                    }
                }
            }
        });
    }

    private void publishEventsWithContext(List<DomainEvent> events, Context otelContextToInject) {
        for (DomainEvent event : events) {
            Map<String, String> metadata = new HashMap<>();
            // Inject the OTel context into the metadata map
            textMapPropagator.inject(otelContextToInject, metadata, mapSetter);

            // Create a new event instance holding the metadata
            DomainEvent eventToPublish = event.withMetadata(metadata);

            log.atInfo()
                    .addKeyValue("eventType", eventToPublish.getClass().getSimpleName()) // Use simple name
                    .addKeyValue("metadataKeys", metadata.keySet())
                    // Optional: Log traceparent for easier debugging if needed
                    .addKeyValue("traceparent", metadata.get("traceparent"))
                    .log("publishing_domain_event_with_metadata");

            applicationEventPublisher.publishEvent(eventToPublish);
        }
        log.atInfo()
                .addKeyValue("eventsCount", events.size())
                .log("domain_events_published");
    }
}