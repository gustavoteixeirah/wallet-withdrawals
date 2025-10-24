package com.teixeirah.withdrawals.application.output;

import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;

import java.util.UUID;

public interface WalletWithdrawOutputPort {

    void save(WalletWithdraw walletWithdraw);

    WalletWithdraw findById(UUID id);

}
