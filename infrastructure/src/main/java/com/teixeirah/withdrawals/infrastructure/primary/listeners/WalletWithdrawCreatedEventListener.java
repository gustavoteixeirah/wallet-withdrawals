package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.application.command.ProcessWalletDebitCommand;
import com.teixeirah.withdrawals.application.input.ProcessWalletDebitInputPort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletWithdrawCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletWithdrawCreatedEventListener {

    private final ProcessWalletDebitInputPort processWalletDebitInputPort;

    @Async
    @EventListener(classes = WalletWithdrawCreatedEvent.class)
    public void handle(WalletWithdrawCreatedEvent event) {
        log.info("Received WalletWithdrawCreatedEvent for withdrawal: {}", event.walletWithdraw().getId());

        try {
            ProcessWalletDebitCommand command = new ProcessWalletDebitCommand(event.walletWithdraw().getId());
            processWalletDebitInputPort.execute(command);

            log.info("Successfully initiated wallet debit for withdrawal: {}", event.walletWithdraw().getId());
        } catch (Exception e) {
            log.error("Failed to process wallet debit for withdrawal: {}", event.walletWithdraw().getId(), e);
            // Note: The domain layer will handle failures and publish failure events
        }
    }
}