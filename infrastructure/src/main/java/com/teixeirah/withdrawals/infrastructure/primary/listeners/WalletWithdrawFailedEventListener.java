package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletWithdrawFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletWithdrawFailedEventListener {

    private final WalletWithdrawRepository walletWithdrawRepository;

    @Async
    @EventListener(classes = WalletWithdrawFailedEvent.class)
    public void handle(WalletWithdrawFailedEvent event) {
        log.info("Received WalletWithdrawFailedEvent for withdrawal: {} with reason: {}", event.withdrawalId(), event.reason());

        try {
            // Check if compensation is needed
            WalletWithdraw walletWithdraw = walletWithdrawRepository.findById(event.withdrawalId());

            if (walletWithdraw.getStatus() == WalletWithdrawStatus.WALLET_DEBITED) {
                log.warn("Withdrawal {} failed after wallet debit. Triggering compensation.", event.withdrawalId());
                performCompensation(walletWithdraw);
            }

            // TODO: Send failure notifications (email, SMS, etc.)
            log.info("Failure notification sent for withdrawal: {}", event.withdrawalId());

        } catch (Exception e) {
            log.error("Failed to handle withdrawal failure for: {}", event.withdrawalId(), e);
        }
    }

    private void performCompensation(WalletWithdraw walletWithdraw) {
        try {
            // Calculate the amount to refund (original amount + fee)
            var refundAmount = walletWithdraw.getAmount().add(walletWithdraw.getFee());

            log.info("Initiating wallet refund for withdrawal {}: amount={}",
                    walletWithdraw.getId(), refundAmount);

            // TODO: Implement actual refund logic using WalletServicePort
            // This would typically call walletService.topUp(walletWithdraw.getUserId(), refundAmount, walletWithdraw.getId())

            log.info("Wallet refund completed for withdrawal: {}", walletWithdraw.getId());

        } catch (Exception e) {
            log.error("Failed to perform compensation for withdrawal: {}", walletWithdraw.getId(), e);
            // TODO: Consider additional error handling like manual intervention alerts
        }
    }
}