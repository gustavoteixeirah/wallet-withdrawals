package com.teixeirah.withdrawals.infrastructure.secondary.persistence;

import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import com.teixeirah.withdrawals.domain.wallet.withdraw.exceptions.WalletWithdrawNotFoundException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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

    @WithSpan(value = "find_wallet_withdraw_by_id_db")
    public WalletWithdraw findById(UUID id) {
        log.atInfo()
           .addKeyValue("id", id)
           .log("wallet_withdraw_find_by_id_started");

        var record = dsl.selectFrom(WALLET_WITHDRAWALS_)
                .where(WALLET_WITHDRAWALS_.ID.eq(id))
                .fetchOne();

        if (record == null) {
            log.atWarn()
               .addKeyValue("id", id)
               .log("wallet_withdraw_not_found");
            throw new WalletWithdrawNotFoundException("Wallet withdraw not found: " + id);
        }

        var recipient = new Recipient(
                record.getRecipientFirstName(),
                record.getRecipientLastName(),
                record.getRecipientNationalId(),
                new Account(record.getRecipientAccountNumber(), record.getRecipientRoutingNumber())
        );

        WalletWithdrawStatus status = WalletWithdrawStatus.valueOf(record.getStatus());

        var walletWithdraw = WalletWithdraw.reconstruct(id, record.getUserId(), record.getAmount(), recipient, status, record.getCreatedAt().toInstant(), record.getFailureReason(), record.getWalletTransactionIdRef(), record.getPaymentProviderIdRef());

        log.atInfo()
           .addKeyValue("id", walletWithdraw.getId())
           .addKeyValue("status", walletWithdraw.getStatus())
           .log("wallet_withdraw_found");
        return walletWithdraw;
    }
}