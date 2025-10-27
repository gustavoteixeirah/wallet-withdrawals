package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.ProcessCompensationCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessCompensationUseCase;
import com.teixeirah.withdrawals.application.usecase.annotations.TransactionalUseCase;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;

import java.util.Objects;

@TransactionalUseCase
public class ProcessCompensationInputPort implements ProcessCompensationUseCase {

    private final WalletWithdrawRepository walletWithdrawRepository;
    private final WalletServicePort walletServicePort;
    private final DomainEventPublisherPort eventPublisher;

    public ProcessCompensationInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            WalletServicePort walletServicePort,
            DomainEventPublisherPort eventPublisher) {
        this.walletWithdrawRepository = Objects.requireNonNull(walletWithdrawRepository);
        this.walletServicePort = Objects.requireNonNull(walletServicePort);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public void execute(final ProcessCompensationCommand command) {

        final var withdrawalId = command.withdrawalId();

        final var walletWithdraw = walletWithdrawRepository.findById(withdrawalId);

        walletWithdraw.attemptCompensation(walletServicePort);

        walletWithdrawRepository.save(walletWithdraw);

        eventPublisher.publish(walletWithdraw.pullDomainEvents());
    }
}