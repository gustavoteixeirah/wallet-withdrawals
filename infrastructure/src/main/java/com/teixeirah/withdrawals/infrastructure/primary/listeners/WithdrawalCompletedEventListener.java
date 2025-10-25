package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WithdrawalCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class WithdrawalCompletedEventListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = WithdrawalCompletedEvent.class)
    public void handle(WithdrawalCompletedEvent event) {
        log.info("Received WithdrawalCompletedEvent for withdrawal: {}", event.withdrawalId());

        // TODO: Implement notification logic (email, SMS, etc.)
        // For now, just log the completion
        log.info("Withdrawal {} completed successfully. Notifications can be sent here.", event.withdrawalId());
    }
}