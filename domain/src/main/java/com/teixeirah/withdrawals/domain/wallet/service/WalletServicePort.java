package com.teixeirah.withdrawals.domain.wallet.service;

import com.teixeirah.withdrawals.domain.wallet.service.exceptions.InsufficientFundsException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletServiceException;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletServicePort {

    Long debit(Long userId, BigDecimal amountToDebit, UUID transactionId)
            throws InsufficientFundsException, WalletNotFoundException, WalletServiceException;

}
