package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.application.command.ProcessPaymentCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessPaymentUseCase;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletDebitedEvent;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletDebitedEventListener {

    private final ProcessPaymentUseCase processPaymentInputPort;
    private final TextMapPropagator textMapPropagator;

    private final TextMapGetter<Map<String, String>> mapGetter = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier != null ? carrier.keySet() : Collections.emptyList();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier != null ? carrier.get(key) : null;
        }
    };

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = WalletDebitedEvent.class)
    public void handle(WalletDebitedEvent event) {
        Map<String, String> metadata = event.getMetadata();
        Context extractedContext = textMapPropagator.extract(Context.current(), metadata, mapGetter);

        try (Scope scope = extractedContext.makeCurrent()) {
            Span listenerSpan = GlobalOpenTelemetry.getTracer("event-listener").spanBuilder("handle_wallet_debited_event").startSpan();
            try (Scope spanScope = listenerSpan.makeCurrent()) {
                log.atDebug()
                   .addKeyValue("traceId", listenerSpan.getSpanContext().getTraceId())
                   .addKeyValue("spanId", listenerSpan.getSpanContext().getSpanId())
                   .addKeyValue("metadataKeys", metadata.keySet())
                   .addKeyValue("traceparent", metadata.get("traceparent"))
                   .log("Listener context activated from metadata");

                log.atInfo()
                        .addKeyValue("withdrawalId", event.withdrawalId())
                        .log("wallet_debited_event_received");

                ProcessPaymentCommand command = new ProcessPaymentCommand(event.withdrawalId());
                processPaymentInputPort.execute(command);

                log.atInfo()
                        .addKeyValue("withdrawalId", event.withdrawalId())
                        .log("payment_processing_initiated");
            } catch (Exception e) {
                log.atError()
                   .addKeyValue("withdrawalId", event.withdrawalId())
                   .setCause(e)
                   .log("wallet_debited_event_handler_error");
                if (listenerSpan.isRecording()) {
                    listenerSpan.recordException(e);
                    listenerSpan.setStatus(StatusCode.ERROR, "Failed handling event");
                }
                throw e;
            } finally {
                listenerSpan.end();
            }
        }
    }
}