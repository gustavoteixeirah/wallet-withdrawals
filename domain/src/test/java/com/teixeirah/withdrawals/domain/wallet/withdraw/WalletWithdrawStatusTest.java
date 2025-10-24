package com.teixeirah.withdrawals.domain.wallet.withdraw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalletWithdrawStatusTest {

    @Test
    void shouldHavePendingStatus() {
        assertNotNull(WalletWithdrawStatus.CREATED);
    }

    @Test
    void shouldHaveCompletedStatus() {
        assertNotNull(WalletWithdrawStatus.COMPLETED);
    }

    @Test
    void shouldHaveFailedStatus() {
        assertNotNull(WalletWithdrawStatus.FAILED);
    }
}
