package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.ProcessWalletDebitCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessWalletDebitUseCase;
import com.teixeirah.withdrawals.application.usecase.annotations.TransactionalUseCase;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;

import java.util.Objects;

@TransactionalUseCase
public class ProcessWalletDebitInputPort implements ProcessWalletDebitUseCase {

    private final WalletWithdrawRepository walletWithdrawRepository;
    private final WalletServicePort walletServicePort;
    private final DomainEventPublisherPort eventPublisher;

    public ProcessWalletDebitInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            WalletServicePort walletServicePort,
            DomainEventPublisherPort eventPublisher) {
        this.walletWithdrawRepository = Objects.requireNonNull(walletWithdrawRepository);
        this.walletServicePort = Objects.requireNonNull(walletServicePort);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public void execute(final ProcessWalletDebitCommand command) {

        final var withdrawalId = command.walletWithdrawId();

        final var walletWithdraw = walletWithdrawRepository.findById(withdrawalId);

        walletWithdraw.processDebit(walletServicePort);

        walletWithdrawRepository.save(walletWithdraw);

        eventPublisher.publish(walletWithdraw.pullDomainEvents());
    }
}
