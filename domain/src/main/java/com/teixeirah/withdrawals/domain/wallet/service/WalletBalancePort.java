package com.teixeirah.withdrawals.domain.wallet.service;

import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletServiceException;

import java.math.BigDecimal;

public interface WalletBalancePort {

    BigDecimal getBalance(Long userId) throws WalletNotFoundException, WalletServiceException;
}
