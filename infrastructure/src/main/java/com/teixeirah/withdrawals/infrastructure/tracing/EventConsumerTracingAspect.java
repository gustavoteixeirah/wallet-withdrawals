package com.teixeirah.withdrawals.infrastructure.tracing;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class EventConsumerTracingAspect {

    private final OtelTracerFacade tracerFacade;

    private final TextMapGetter<DomainEvent> eventMetadataGetter =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(DomainEvent carrier) {
                    return carrier.getMetadata().keySet();
                }

                @Override
                @Nullable
                public String get(@Nullable DomainEvent carrier, String key) {
                    return carrier == null ? null : carrier.getMetadata().get(key);
                }
            };

    public EventConsumerTracingAspect(OtelTracerFacade tracerFacade) {
        this.tracerFacade = tracerFacade;
    }

    @Around("(@annotation(org.springframework.context.event.EventListener) || @annotation(org.springframework.transaction.event.TransactionalEventListener)) && args(event)")
    public Object traceEventConsumer(ProceedingJoinPoint joinPoint, Object event) throws Throwable {

        if (!(event instanceof DomainEvent domainEvent)) {
            return joinPoint.proceed(); // Not a DomainEvent, do not trace
        }

        String spanName = "consume." + event.getClass().getSimpleName();

        return tracerFacade.newConsumerSpan(
                spanName,
                domainEvent,
                eventMetadataGetter,
                joinPoint::proceed
        );
    }
}