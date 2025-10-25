package com.teixeirah.withdrawals.application.usecase;

import com.teixeirah.withdrawals.application.command.InitiateWalletWithdrawalCommand;
import com.teixeirah.withdrawals.application.response.InitiateWalletWithdrawalResponse;

public interface InitiateWalletWithdrawalUseCase {

    InitiateWalletWithdrawalResponse execute(final InitiateWalletWithdrawalCommand command);

}
