package com.teixeirah.withdrawals.infrastructure.support;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.jooq.DSLContext;

import static org.jooq.generated.wallet_withdrawals.Tables.WALLET_WITHDRAWALS_;

public final class WalletWithdrawalTestDataBuilder {

    private UUID id = UUID.randomUUID();
    private long userId = 1L;
    private String status = "CREATED";
    private BigDecimal amount = BigDecimal.valueOf(100.00);
    private BigDecimal fee = BigDecimal.valueOf(10.00);
    private BigDecimal amountForRecipient = BigDecimal.valueOf(90.00);
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    private String walletTransactionIdRef = "wallet-tx-123";
    private String recipientFirstName = "Test";
    private String recipientLastName = "User";
    private String recipientNationalId = "12345678901";
    private String recipientAccountNumber = "987654321";
    private String recipientRoutingNumber = "123456789";

    public static WalletWithdrawalTestDataBuilder walletWithdrawal() {
        return new WalletWithdrawalTestDataBuilder();
    }

    public WalletWithdrawalTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public WalletWithdrawalTestDataBuilder withStatus(String status) {
        this.status = status;
        return this;
    }

    public WalletWithdrawalTestDataBuilder withWalletTransactionIdRef(String walletTransactionIdRef) {
        this.walletTransactionIdRef = walletTransactionIdRef;
        return this;
    }

    public WalletWithdrawalTestDataBuilder withRecipient(String firstName, String lastName) {
        this.recipientFirstName = firstName;
        this.recipientLastName = lastName;
        return this;
    }

    public WalletWithdrawalTestDataBuilder withRecipientAccount(String accountNumber, String routingNumber) {
        this.recipientAccountNumber = accountNumber;
        this.recipientRoutingNumber = routingNumber;
        return this;
    }

    public WalletWithdrawalTestDataBuilder withAmounts(BigDecimal amount, BigDecimal fee, BigDecimal amountForRecipient) {
        this.amount = amount;
        this.fee = fee;
        this.amountForRecipient = amountForRecipient;
        return this;
    }

    public WalletWithdrawalTestDataBuilder withTimestamps(OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        return this;
    }

    public UUID getId() {
        return id;
    }

    public void insert(DSLContext dsl) {
        dsl.insertInto(WALLET_WITHDRAWALS_)
                .set(WALLET_WITHDRAWALS_.ID, id)
                .set(WALLET_WITHDRAWALS_.USER_ID, userId)
                .set(WALLET_WITHDRAWALS_.STATUS, status)
                .set(WALLET_WITHDRAWALS_.AMOUNT, amount)
                .set(WALLET_WITHDRAWALS_.FEE, fee)
                .set(WALLET_WITHDRAWALS_.AMOUNT_FOR_RECIPIENT, amountForRecipient)
                .set(WALLET_WITHDRAWALS_.CREATED_AT, createdAt)
                .set(WALLET_WITHDRAWALS_.UPDATED_AT, updatedAt)
                .set(WALLET_WITHDRAWALS_.WALLET_TRANSACTION_ID_REF, walletTransactionIdRef)
                .set(WALLET_WITHDRAWALS_.RECIPIENT_FIRST_NAME, recipientFirstName)
                .set(WALLET_WITHDRAWALS_.RECIPIENT_LAST_NAME, recipientLastName)
                .set(WALLET_WITHDRAWALS_.RECIPIENT_NATIONAL_ID, recipientNationalId)
                .set(WALLET_WITHDRAWALS_.RECIPIENT_ACCOUNT_NUMBER, recipientAccountNumber)
                .set(WALLET_WITHDRAWALS_.RECIPIENT_ROUTING_NUMBER, recipientRoutingNumber)
                .execute();
    }
}
