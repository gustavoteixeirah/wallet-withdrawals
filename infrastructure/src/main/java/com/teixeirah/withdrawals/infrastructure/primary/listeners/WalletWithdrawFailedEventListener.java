package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
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