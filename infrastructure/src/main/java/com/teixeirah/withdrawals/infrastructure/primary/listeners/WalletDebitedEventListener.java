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
        log.info("Received WalletDebitedEvent for withdrawal: {}", event.withdrawalId());

        if (Math.random() < 0.5) {
            log.error("Simulating transient failure for withdrawal: {}", event.withdrawalId());
            throw new RuntimeException();
        }

        ProcessPaymentCommand command = new ProcessPaymentCommand(event.withdrawalId());
        processPaymentInputPort.execute(command);

        log.info("Successfully initiated payment processing for withdrawal: {}", event.withdrawalId());
    }
}