package com.teixeirah.withdrawals.infrastructure.secondary.persistence;

import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletWithdrawRepositoryAdapter implements WalletWithdrawRepository {

    private final WalletWithdrawJooqReadAdapter readAdapter;
    private final WalletWithdrawJooqWriteAdapter writeAdapter;

    @Override
    public void save(WalletWithdraw walletWithdraw) {
        writeAdapter.save(walletWithdraw);
    }

    @Override
    public WalletWithdraw findById(UUID id) {
        return readAdapter.findById(id);
    }
}