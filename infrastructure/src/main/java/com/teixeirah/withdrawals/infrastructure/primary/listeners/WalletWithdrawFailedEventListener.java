package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletWithdrawFailedEvent;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletWithdrawFailedEventListener {

    private final WalletWithdrawRepository walletWithdrawRepository;

    @Async
    @WithSpan(value = "Received WalletWithdrawFailedEvent to mark withdrawal as failed")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = WalletWithdrawFailedEvent.class)
    public void handle(WalletWithdrawFailedEvent event) {
        log.atInfo()
           .addKeyValue("withdrawalId", event.withdrawalId())
           .addKeyValue("reason", event.reason())
           .log("wallet_withdraw_failed_event_received");

        // TODO: Send failure notifications (email, SMS, etc.) for non-compensatable failures
        log.atInfo()
           .addKeyValue("withdrawalId", event.withdrawalId())
           .log("withdrawal_failure_notification_sent");
    }
}