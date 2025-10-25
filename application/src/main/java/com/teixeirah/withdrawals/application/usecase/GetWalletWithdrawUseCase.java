package com.teixeirah.withdrawals.application.usecase;

import com.teixeirah.withdrawals.application.response.WalletWithdrawResponse;

import java.util.UUID;

public interface GetWalletWithdrawUseCase {

    WalletWithdrawResponse execute(UUID id);

}