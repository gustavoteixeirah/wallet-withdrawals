package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletWithdrawFailedEvent;
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
public class WalletWithdrawFailedEventListener {

    private final WalletWithdrawRepository walletWithdrawRepository;
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = WalletWithdrawFailedEvent.class)
    public void handle(WalletWithdrawFailedEvent event) {
        Map<String, String> metadata = event.getMetadata();
        Context extractedContext = textMapPropagator.extract(Context.current(), metadata, mapGetter);

        try (Scope scope = extractedContext.makeCurrent()) {
            Span listenerSpan = GlobalOpenTelemetry.getTracer("event-listener").spanBuilder("handle_wallet_withdraw_failed_event").startSpan();
            try (Scope spanScope = listenerSpan.makeCurrent()) {
                log.atDebug()
                   .addKeyValue("traceId", listenerSpan.getSpanContext().getTraceId())
                   .addKeyValue("spanId", listenerSpan.getSpanContext().getSpanId())
                   .addKeyValue("metadataKeys", metadata.keySet())
                   .addKeyValue("traceparent", metadata.get("traceparent"))
                   .log("Listener context activated from metadata");

                log.atInfo()
               .addKeyValue("withdrawalId", event.withdrawalId())
               .addKeyValue("reason", event.reason())
               .log("wallet_withdraw_failed_event_received");

            WalletWithdraw walletWithdraw = walletWithdrawRepository.findById(event.withdrawalId());

            if (walletWithdraw.getStatus() == WalletWithdrawStatus.WALLET_DEBITED) {
                log.atWarn()
                   .addKeyValue("withdrawalId", event.withdrawalId())
                   .log("withdrawal_failed_after_wallet_debit_compensation_triggered");
                performCompensation(walletWithdraw);
            }

            // TODO: Send failure notifications (email, SMS, etc.)
            log.atInfo()
               .addKeyValue("withdrawalId", event.withdrawalId())
               .log("withdrawal_failure_notification_sent");
            } catch (Exception e) {
                log.atError()
               .addKeyValue("withdrawalId", event.withdrawalId())
               .setCause(e)
               .log("wallet_withdraw_failed_handler_error");
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

    private void performCompensation(WalletWithdraw walletWithdraw) {
        try {
            // Calculate the amount to refund (original amount + fee)
            var refundAmount = walletWithdraw.getAmount().add(walletWithdraw.getFee());

            log.atInfo()
               .addKeyValue("withdrawalId", walletWithdraw.getId())
               .addKeyValue("refundAmount", refundAmount)
               .log("wallet_refund_initiated");

            // TODO: Implement actual refund logic using WalletServicePort
            // This would typically call walletService.topUp(walletWithdraw.getUserId(), refundAmount, walletWithdraw.getId())

            log.atInfo()
               .addKeyValue("withdrawalId", walletWithdraw.getId())
               .log("wallet_refund_completed");

        } catch (Exception e) {
            log.atError()
               .addKeyValue("withdrawalId", walletWithdraw.getId())
               .setCause(e)
               .log("wallet_refund_compensation_failed");
            // TODO: Consider additional error handling like manual intervention alerts
        }
    }
}