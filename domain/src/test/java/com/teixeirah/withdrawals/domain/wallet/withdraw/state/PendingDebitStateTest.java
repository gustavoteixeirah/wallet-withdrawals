package com.teixeirah.withdrawals.domain.wallet.withdraw.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PendingDebitStateTest {

    @Test
    void shouldCreatePendingState() {
        PendingDebitState state = new PendingDebitState();

        assertNotNull(state);
        assertInstanceOf(WalletWithdrawState.class, state);
    }
}
