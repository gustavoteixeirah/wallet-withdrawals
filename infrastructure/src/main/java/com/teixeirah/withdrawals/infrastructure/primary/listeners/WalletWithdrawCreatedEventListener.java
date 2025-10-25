package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.application.command.ProcessWalletDebitCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessWalletDebitUseCase;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletWithdrawCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletWithdrawCreatedEventListener {

    private final ProcessWalletDebitUseCase processWalletDebitInputPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = WalletWithdrawCreatedEvent.class)
    public void handle(WalletWithdrawCreatedEvent event) {
        log.atInfo()
           .addKeyValue("withdrawalId", event.walletWithdraw().getId())
           .log("wallet_withdraw_created_event_received");

        ProcessWalletDebitCommand command = new ProcessWalletDebitCommand(event.walletWithdraw().getId());
        processWalletDebitInputPort.execute(command);

        log.atInfo()
           .addKeyValue("withdrawalId", event.walletWithdraw().getId())
           .log("wallet_debit_initiated");
    }
}