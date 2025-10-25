package com.teixeirah.withdrawals.application.usecase;

import com.teixeirah.withdrawals.application.command.GetWalletWithdrawCommand;
import com.teixeirah.withdrawals.application.response.WalletWithdrawResponse;

public interface GetWalletWithdrawUseCase extends UseCase<GetWalletWithdrawCommand, WalletWithdrawResponse> {

}