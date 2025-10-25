package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.InitiateWalletWithdrawalCommand;
import com.teixeirah.withdrawals.application.response.InitiateWalletWithdrawalResponse;
import com.teixeirah.withdrawals.application.usecase.InitiateWalletWithdrawalUseCase;
import com.teixeirah.withdrawals.application.usecase.annotations.TransactionalUseCase;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;

import java.util.Objects;

@TransactionalUseCase
public class InitiateWalletWithdrawInputPort implements InitiateWalletWithdrawalUseCase {

    private final WalletWithdrawRepository walletWithdrawRepository;
    private final DomainEventPublisherPort eventPublisher;

    public InitiateWalletWithdrawInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            DomainEventPublisherPort eventPublisher) {
        this.walletWithdrawRepository = Objects.requireNonNull(walletWithdrawRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public InitiateWalletWithdrawalResponse execute(final InitiateWalletWithdrawalCommand command) {

        final var recipientAccount = new Account(command.recipientAccountNumber(), command.recipientRoutingNumber());
        final var recipient = new Recipient(
                command.recipientFirstName(),
                command.recipientLastName(),
                command.recipientNationalId(),
                recipientAccount
        );

        final var walletWithdraw = WalletWithdrawOperations.create(command.userId(), command.amount(), recipient);

        walletWithdrawRepository.save(walletWithdraw);

        eventPublisher.publish(walletWithdraw.pullDomainEvents());

        return mapToResponse(walletWithdraw);
    }

    private InitiateWalletWithdrawalResponse mapToResponse(WalletWithdraw walletWithdraw) {
        return new InitiateWalletWithdrawalResponse(
                walletWithdraw.getId(),
                walletWithdraw.getStatus(),
                walletWithdraw.getCreatedAt()
        );
    }
}
