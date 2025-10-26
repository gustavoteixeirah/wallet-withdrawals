package com.teixeirah.withdrawals.infrastructure.secondary.messaging.configuration;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.infrastructure.tracing.OtelTracerFacade;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
class TracingDomainEventPublisherDecorator implements DomainEventPublisherPort {

    private final DomainEventPublisherPort delegate;
    private final OtelTracerFacade tracerFacade;

    private static final TextMapSetter<DomainEvent> eventMetadataSetter =
            (carrier, key, value) -> {
                if (carrier != null) {
                    carrier.setMetadata(key, value);
                }
            };

    public TracingDomainEventPublisherDecorator(
            @Qualifier("defaultEventPublisher") DomainEventPublisherPort delegate,
            OtelTracerFacade tracerFacade
    ) {
        this.delegate = delegate;
        this.tracerFacade = tracerFacade;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        events.forEach(event -> tracerFacade.injectCurrentContext(event, eventMetadataSetter));
        delegate.publish(events);
    }
}