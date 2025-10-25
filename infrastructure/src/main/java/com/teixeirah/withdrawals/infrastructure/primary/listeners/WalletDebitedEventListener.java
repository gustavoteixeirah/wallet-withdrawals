package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.application.command.ProcessPaymentCommand;
import com.teixeirah.withdrawals.application.input.ProcessPaymentInputPort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletDebitedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletDebitedEventListener {

    private final ProcessPaymentInputPort processPaymentInputPort;

    @Async
    @EventListener(classes = WalletDebitedEvent.class)
    public void handle(WalletDebitedEvent event) {
        log.info("Received WalletDebitedEvent for withdrawal: {}", event.withdrawalId());

        try {
            ProcessPaymentCommand command = new ProcessPaymentCommand(event.withdrawalId());
            processPaymentInputPort.execute(command);

            log.info("Successfully initiated payment processing for withdrawal: {}", event.withdrawalId());
        } catch (Exception e) {
            log.error("Failed to process payment for withdrawal: {}", event.withdrawalId(), e);
            // Note: The domain layer will handle failures and publish failure events
        }
    }
}