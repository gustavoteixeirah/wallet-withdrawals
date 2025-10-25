package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.GetWalletWithdrawCommand;
import com.teixeirah.withdrawals.application.response.WalletWithdrawResponse;
import com.teixeirah.withdrawals.application.usecase.GetWalletWithdrawUseCase;
import com.teixeirah.withdrawals.application.usecase.annotations.TransactionalUseCase;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;

import java.util.Objects;

@TransactionalUseCase
public class GetWalletWithdrawInputPort implements GetWalletWithdrawUseCase {

    private final WalletWithdrawRepository walletWithdrawRepository;

    public GetWalletWithdrawInputPort(WalletWithdrawRepository walletWithdrawRepository) {
        this.walletWithdrawRepository = Objects.requireNonNull(walletWithdrawRepository);
    }

    @Override
    public WalletWithdrawResponse execute(final GetWalletWithdrawCommand command) {
        WalletWithdraw walletWithdraw = walletWithdrawRepository.findById(command.id());
        return mapToResponse(walletWithdraw);
    }

    private WalletWithdrawResponse mapToResponse(WalletWithdraw walletWithdraw) {
        return new WalletWithdrawResponse(
                walletWithdraw.getId(),
                walletWithdraw.getUserId(),
                walletWithdraw.getAmount(),
                walletWithdraw.getFee(),
                walletWithdraw.getAmountForRecipient(),
                walletWithdraw.getStatus(),
                walletWithdraw.getCreatedAt(),
                walletWithdraw.getFailureReason(),
                walletWithdraw.getWalletTransactionIdRef(),
                walletWithdraw.getPaymentProviderIdRef(),
                walletWithdraw.getRecipient().firstName(),
                walletWithdraw.getRecipient().lastName(),
                walletWithdraw.getRecipient().nationalId(),
                walletWithdraw.getRecipient().account().accountNumber(),
                walletWithdraw.getRecipient().account().routingNumber()
        );
    }
}