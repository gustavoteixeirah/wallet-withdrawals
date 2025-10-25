package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.response.WalletWithdrawResponse;
import com.teixeirah.withdrawals.application.usecase.GetWalletWithdrawUseCase;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;

import java.util.Objects;
import java.util.UUID;

public class GetWalletWithdrawInputPort implements GetWalletWithdrawUseCase {

    private final WalletWithdrawRepository walletWithdrawRepository;

    public GetWalletWithdrawInputPort(WalletWithdrawRepository walletWithdrawRepository) {
        this.walletWithdrawRepository = Objects.requireNonNull(walletWithdrawRepository);
    }

    @Override
    public WalletWithdrawResponse execute(UUID id) {
        WalletWithdraw walletWithdraw = walletWithdrawRepository.findById(id);
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