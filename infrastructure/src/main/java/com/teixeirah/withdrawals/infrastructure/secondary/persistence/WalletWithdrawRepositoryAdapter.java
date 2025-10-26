package com.teixeirah.withdrawals.infrastructure.secondary.persistence;

import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletWithdrawRepositoryAdapter implements WalletWithdrawRepository {

    private final WalletWithdrawJooqReadAdapter readAdapter;
    private final WalletWithdrawJooqWriteAdapter writeAdapter;

    @Override
    @WithSpan(value = "Persisting WalletWithdraw entity")
    public void save(WalletWithdraw walletWithdraw) {
        writeAdapter.save(walletWithdraw);
    }

    @Override
    @WithSpan(value = "Retrieving WalletWithdraw entity by ID")
    public WalletWithdraw findById(UUID id) {
        return readAdapter.findById(id);
    }
}