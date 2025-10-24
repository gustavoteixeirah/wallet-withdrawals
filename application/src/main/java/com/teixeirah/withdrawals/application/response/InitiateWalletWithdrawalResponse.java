package com.teixeirah.withdrawals.application.response;

import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;

import java.time.Instant;
import java.util.UUID;

public record InitiateWalletWithdrawalResponse(UUID transactionId,
                                               WalletWithdrawStatus status,
                                               Instant createdAt) {

}
