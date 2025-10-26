package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WithdrawalCompletedEvent;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class WithdrawalCompletedEventListener {

    @Async
    @WithSpan(value = "Received WithdrawalCompletedEvent to send notification")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = WithdrawalCompletedEvent.class)
    public void handle(WithdrawalCompletedEvent event) {
        log.atInfo()
           .addKeyValue("withdrawalId", event.withdrawalId())
           .log("withdrawal_completed_event_received");

        // TODO: Implement notification logic (email, SMS, etc.)
        log.atInfo()
           .addKeyValue("withdrawalId", event.withdrawalId())
           .log("withdrawal_completed_notification_pending");
    }
}