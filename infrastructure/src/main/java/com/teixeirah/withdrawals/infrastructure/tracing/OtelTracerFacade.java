package com.teixeirah.withdrawals.infrastructure.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.stereotype.Component;

@Component
public class OtelTracerFacade {

    private final Tracer tracer;
    private final ContextPropagators contextPropagators;

    @FunctionalInterface
    public interface ThrowableSupplier<T> {
        T get() throws Throwable;
    }

    public OtelTracerFacade(Tracer tracer, ContextPropagators contextPropagators) {
        this.tracer = tracer;
        this.contextPropagators = contextPropagators;
    }

    public <T> T newSpan(String spanName, ThrowableSupplier<T> task) throws Throwable {
        Span span = tracer.spanBuilder(spanName)
                .setParent(Context.current())
                .startSpan();
        return executeWithSpan(span, task);
    }

    public <T, C> T newConsumerSpan(String spanName, C carrier, TextMapGetter<C> getter, ThrowableSupplier<T> task) throws Throwable {
        // Extract context from the carrier
        Context extractedContext = contextPropagators.getTextMapPropagator()
                .extract(Context.current(), carrier, getter);

        Span span = tracer.spanBuilder(spanName)
                .setParent(extractedContext)
                .setSpanKind(SpanKind.CONSUMER)
                .startSpan();

        // Execute the task within the new span's scope
        return executeWithSpan(span, task);
    }

    public <T, C> T newProducerSpan(String spanName, C carrier, TextMapSetter<C> setter, ThrowableSupplier<T> task) throws Throwable {
        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.PRODUCER)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Inject the new span's context into the carrier
            contextPropagators.getTextMapPropagator()
                    .inject(Context.current(), carrier, setter);

            // Execute the task (e.g., the HTTP call)
            return task.get();
        } catch (Throwable e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    public <C> void injectCurrentContext(C carrier, TextMapSetter<C> setter) {
        // Inject the currently active context
        contextPropagators.getTextMapPropagator()
                .inject(Context.current(), carrier, setter);
    }

    private <T> T executeWithSpan(Span span, ThrowableSupplier<T> task) throws Throwable {
        try (Scope scope = span.makeCurrent()) {
            return task.get();
        } catch (Throwable e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}