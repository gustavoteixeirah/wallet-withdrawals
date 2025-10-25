package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.application.command.ProcessPaymentCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessPaymentUseCase;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletDebitedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletDebitedEventListener {

    private final ProcessPaymentUseCase processPaymentInputPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = WalletDebitedEvent.class)
    public void handle(WalletDebitedEvent event) {
        log.atInfo()
           .addKeyValue("withdrawalId", event.withdrawalId())
           .log("wallet_debited_event_received");

        ProcessPaymentCommand command = new ProcessPaymentCommand(event.withdrawalId());
        processPaymentInputPort.execute(command);

        log.atInfo()
           .addKeyValue("withdrawalId", event.withdrawalId())
           .log("payment_processing_initiated");
    }
}