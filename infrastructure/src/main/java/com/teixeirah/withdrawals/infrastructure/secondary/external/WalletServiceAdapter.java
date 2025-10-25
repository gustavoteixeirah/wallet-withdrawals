package com.teixeirah.withdrawals.infrastructure.secondary.external;

import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.InsufficientFundsException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class WalletServiceAdapter implements WalletServicePort {

    @Override
    public Long debit(Long userId, BigDecimal amount, UUID transactionId) throws InsufficientFundsException, WalletNotFoundException, WalletServiceException {
        log.info("Debiting wallet: userId={}, amount={}, transactionId={}", userId, amount, transactionId);

        // Simulate successful debit - return a transaction ID as Long
        Long walletTransactionId = System.currentTimeMillis();
        log.info("Wallet debited successfully with transaction ID: {}", walletTransactionId);

        return walletTransactionId;
    }
}