package com.teixeirah.withdrawals.infrastructure.primary.listeners;

import com.teixeirah.withdrawals.application.command.ProcessCompensationCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessCompensationUseCase;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletCompensationPendingEvent;
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
public class WalletCompensationPendingEventListener {

    private final ProcessCompensationUseCase processCompensationUseCase;

    @Async
    @WithSpan(value = "Received WalletCompensationPendingEvent to trigger compensation")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, classes = WalletCompensationPendingEvent.class)
    public void handle(WalletCompensationPendingEvent event) {
        log.atInfo()
           .addKeyValue("withdrawalId", event.withdrawalId())
           .addKeyValue("refundAmount", event.refundAmount())
           .log("wallet_compensation_pending_event_received");

        try {
            processCompensationUseCase.execute(new ProcessCompensationCommand(event.withdrawalId()));
        } catch (Exception e) {
            log.atError()
               .addKeyValue("withdrawalId", event.withdrawalId())
               .setCause(e)
               .log("compensation_use_case_failed");
            // Aggregate remains in COMPENSATION_PENDING for manual intervention
        }
    }
}