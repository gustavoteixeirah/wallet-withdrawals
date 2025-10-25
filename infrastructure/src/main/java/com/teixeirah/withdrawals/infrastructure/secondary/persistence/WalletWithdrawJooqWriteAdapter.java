package com.teixeirah.withdrawals.infrastructure.secondary.persistence;

import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import static org.jooq.generated.wallet_withdrawals.Tables.WALLET_WITHDRAWALS_;


@Slf4j
@Repository
class WalletWithdrawJooqWriteAdapter {

    private final DSLContext writeDsl;

    WalletWithdrawJooqWriteAdapter(@Qualifier("writeDsl") DSLContext writeDsl) {
        this.writeDsl = writeDsl;
    }

    @WithSpan(value = "save_wallet_withdraw_db")
    public void save(WalletWithdraw walletWithdraw) {
        log.atInfo()
           .addKeyValue("id", walletWithdraw.getId())
           .addKeyValue("status", walletWithdraw.getStatus())
           .log("wallet_withdraw_save_started");

        writeDsl.insertInto(WALLET_WITHDRAWALS_)
                .set(WALLET_WITHDRAWALS_.ID, walletWithdraw.getId())
                .set(WALLET_WITHDRAWALS_.USER_ID, walletWithdraw.getUserId())
                .set(WALLET_WITHDRAWALS_.STATUS, walletWithdraw.getStatus().name())
                .set(WALLET_WITHDRAWALS_.AMOUNT, walletWithdraw.getAmount())
                .set(WALLET_WITHDRAWALS_.FEE, walletWithdraw.getFee())
                .set(WALLET_WITHDRAWALS_.AMOUNT_FOR_RECIPIENT, walletWithdraw.getAmount().subtract(walletWithdraw.getFee()))
                .set(WALLET_WITHDRAWALS_.CREATED_AT, java.time.OffsetDateTime.ofInstant(walletWithdraw.getCreatedAt(), java.time.ZoneOffset.UTC))
                .set(WALLET_WITHDRAWALS_.UPDATED_AT, java.time.OffsetDateTime.now())
                .set(WALLET_WITHDRAWALS_.RECIPIENT_FIRST_NAME, walletWithdraw.getRecipient().firstName())
                .set(WALLET_WITHDRAWALS_.RECIPIENT_LAST_NAME, walletWithdraw.getRecipient().lastName())
                .set(WALLET_WITHDRAWALS_.RECIPIENT_NATIONAL_ID, walletWithdraw.getRecipient().nationalId())
                .set(WALLET_WITHDRAWALS_.RECIPIENT_ACCOUNT_NUMBER, walletWithdraw.getRecipient().account().accountNumber())
                .set(WALLET_WITHDRAWALS_.RECIPIENT_ROUTING_NUMBER, walletWithdraw.getRecipient().account().routingNumber())
                .onDuplicateKeyUpdate()
                .set(WALLET_WITHDRAWALS_.STATUS, walletWithdraw.getStatus().name())
                .set(WALLET_WITHDRAWALS_.UPDATED_AT, java.time.OffsetDateTime.now())
                .set(WALLET_WITHDRAWALS_.FAILURE_REASON, walletWithdraw.getFailureReason())
                .set(WALLET_WITHDRAWALS_.WALLET_TRANSACTION_ID_REF, walletWithdraw.getWalletTransactionIdRef())
                .set(WALLET_WITHDRAWALS_.PAYMENT_PROVIDER_ID_REF, walletWithdraw.getPaymentProviderIdRef())
                .execute();

        log.atInfo()
           .addKeyValue("id", walletWithdraw.getId())
           .log("wallet_withdraw_saved");
    }
}