package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.InitiateWalletWithdrawalCommand;
import com.teixeirah.withdrawals.application.output.WalletWithdrawOutputPort;
import com.teixeirah.withdrawals.application.response.InitiateWalletWithdrawalResponse;
import com.teixeirah.withdrawals.application.usecase.InitiateWalletWithdrawalUseCase;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;

public class InitiateWalletWithdrawInputPort implements InitiateWalletWithdrawalUseCase {

    private final WalletWithdrawOutputPort walletWithdrawOutputPort;
    private final DomainEventPublisherPort domainEventPublisher;
    private final WalletWithdrawOperations walletWithdrawOperations;

    public InitiateWalletWithdrawInputPort(WalletWithdrawOutputPort walletWithdrawOutputPort,
                                           DomainEventPublisherPort domainEventPublisher,
                                           WalletWithdrawOperations walletWithdrawOperations) {
        this.walletWithdrawOutputPort = walletWithdrawOutputPort;
        this.domainEventPublisher = domainEventPublisher;
        this.walletWithdrawOperations = walletWithdrawOperations;
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

        final var walletWithdraw = walletWithdrawOperations.create(command.userId(), command.amount(), recipient);

        walletWithdrawOutputPort.save(walletWithdraw);

        domainEventPublisher.publish(walletWithdraw.pullDomainEvents());

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
