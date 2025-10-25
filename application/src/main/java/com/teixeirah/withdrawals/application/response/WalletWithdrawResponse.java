package com.teixeirah.withdrawals.application.response;

import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletWithdrawResponse(
        UUID id,
        Long userId,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal amountForRecipient,
        WalletWithdrawStatus status,
        Instant createdAt,
        String failureReason,
        String walletTransactionIdRef,
        String paymentProviderIdRef,
        String recipientFirstName,
        String recipientLastName,
        String recipientNationalId,
        String recipientAccountNumber,
        String recipientRoutingNumber
) {
}