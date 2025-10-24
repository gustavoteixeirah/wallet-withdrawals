package com.teixeirah.withdrawals.domain.wallet.withdraw;

import java.util.UUID;

public interface WalletWithdrawRepository {

    void save(WalletWithdraw walletWithdraw);

    WalletWithdraw findById(UUID id);

}
