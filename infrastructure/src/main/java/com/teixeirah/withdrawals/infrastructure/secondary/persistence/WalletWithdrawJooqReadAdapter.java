package com.teixeirah.withdrawals.infrastructure.secondary.persistence;

import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static org.jooq.generated.wallet_withdrawals.Tables.WALLET_WITHDRAWALS_;

@Slf4j
@Repository
public class WalletWithdrawJooqReadAdapter {

    private final DSLContext dsl;

    public WalletWithdrawJooqReadAdapter(@Qualifier("readDsl") DSLContext dsl) {
        this.dsl = dsl;
    }

    public WalletWithdraw findById(UUID id) {
        log.info("Finding wallet withdraw by id: {}", id);

        var record = dsl.selectFrom(WALLET_WITHDRAWALS_)
                .where(WALLET_WITHDRAWALS_.ID.eq(id))
                .fetchOne();

        if (record == null) {
            log.warn("Wallet withdraw not found: {}", id);
            throw new RuntimeException("Wallet withdraw not found: " + id);
        }

        var recipient = new Recipient(
                record.getRecipientFirstName(),
                record.getRecipientLastName(),
                record.getRecipientNationalId(),
                new Account(record.getRecipientAccountNumber(), record.getRecipientRoutingNumber())
        );

        WalletWithdrawStatus status = WalletWithdrawStatus.valueOf(record.getStatus());

        // Reconstruct the WalletWithdraw
        var walletWithdraw = WalletWithdraw.reconstruct(id, record.getUserId(), record.getAmount(), recipient, status, record.getCreatedAt().toInstant(), record.getFailureReason(), record.getWalletTransactionIdRef(), record.getPaymentProviderIdRef());

        log.info("Wallet withdraw found: {}", walletWithdraw.getId());
        return walletWithdraw;
    }
}