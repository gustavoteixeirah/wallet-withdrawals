package com.teixeirah.withdrawals.domain.wallet.withdraw;

import com.teixeirah.withdrawals.domain.wallet.withdraw.exceptions.WalletWithdrawNotFoundException;

import java.util.UUID;

public interface WalletWithdrawRepository {

    void save(WalletWithdraw walletWithdraw);

    WalletWithdraw findById(UUID id) throws WalletWithdrawNotFoundException;

}
