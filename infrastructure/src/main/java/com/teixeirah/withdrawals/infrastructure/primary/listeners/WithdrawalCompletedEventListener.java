package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WithdrawalCompletedEvent;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalCompletedEventListener {

    private final LongCounter successWithdrawalStatusCounter;

    @Async
    @WithSpan(value = "Received WithdrawalCompletedEvent to send notification")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = WithdrawalCompletedEvent.class)
    public void handle(WithdrawalCompletedEvent event) {
        log.atInfo()
                .addKeyValue("withdrawalId", event.withdrawalId())
                .log("withdrawal_completed_event_received");

        successWithdrawalStatusCounter.add(1, Attributes.of(stringKey("status"), "COMPLETED"));

        log.atInfo()
                .addKeyValue("withdrawalId", event.withdrawalId())
                .log("withdrawal_completed_notification_pending");
    }
}