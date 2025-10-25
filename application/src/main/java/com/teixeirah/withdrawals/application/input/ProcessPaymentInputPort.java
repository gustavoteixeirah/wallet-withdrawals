package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.ProcessPaymentCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessPaymentUseCase;
import com.teixeirah.withdrawals.application.usecase.annotations.TransactionalUseCase;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;

import java.util.Objects;

@TransactionalUseCase
public class ProcessPaymentInputPort implements ProcessPaymentUseCase {

    private final WalletWithdrawRepository walletWithdrawRepository;
    private final PaymentProviderPort paymentProviderPort;
    private final PaymentSourceProviderPort paymentSourceProviderPort;
    private final DomainEventPublisherPort eventPublisher;

    public ProcessPaymentInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            PaymentProviderPort paymentProviderPort,
            PaymentSourceProviderPort paymentSourceProviderPort,
            DomainEventPublisherPort eventPublisher) {
        this.walletWithdrawRepository = Objects.requireNonNull(walletWithdrawRepository);
        this.paymentProviderPort = Objects.requireNonNull(paymentProviderPort);
        this.paymentSourceProviderPort = Objects.requireNonNull(paymentSourceProviderPort);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public void execute(final ProcessPaymentCommand command) {

        final var withdrawalId = command.withdrawalId();

        final var walletWithdraw = walletWithdrawRepository.findById(withdrawalId);

        walletWithdraw.processPayment(paymentProviderPort, paymentSourceProviderPort);

        walletWithdrawRepository.save(walletWithdraw);

        eventPublisher.publish(walletWithdraw.pullDomainEvents());
    }
}
