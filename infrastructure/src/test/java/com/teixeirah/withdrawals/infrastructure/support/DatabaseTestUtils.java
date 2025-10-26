package com.teixeirah.withdrawals.infrastructure.support;

import org.jooq.DSLContext;

import static org.jooq.generated.wallet_withdrawals.Tables.WALLET_WITHDRAWALS_;

public final class DatabaseTestUtils {

    private DatabaseTestUtils() {
    }

    public static void resetWalletWithdrawalsTable(DSLContext dsl) {
        dsl.deleteFrom(WALLET_WITHDRAWALS_).execute();
    }
}
