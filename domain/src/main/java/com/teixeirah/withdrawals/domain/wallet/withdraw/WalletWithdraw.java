package com.teixeirah.withdrawals.domain.wallet.withdraw;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.service.WalletBalancePort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletServiceException;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletCompensationPendingEvent;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletRefundCompletedEvent;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletWithdrawFailedEvent;
import com.teixeirah.withdrawals.domain.wallet.withdraw.state.CompensationPendingState;
import com.teixeirah.withdrawals.domain.wallet.withdraw.state.CompletedState;
import com.teixeirah.withdrawals.domain.wallet.withdraw.state.FailedState;
import com.teixeirah.withdrawals.domain.wallet.withdraw.state.PendingDebitState;
import com.teixeirah.withdrawals.domain.wallet.withdraw.state.WalletDebitedState;
import com.teixeirah.withdrawals.domain.wallet.withdraw.state.WalletWithdrawState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WalletWithdraw {

    private final UUID id;
    private final Recipient recipient;
    private final Long userId;
    private final BigDecimal amount;
    private final BigDecimal fee;
    private Instant createdAt = Instant.now();

    private WalletWithdrawStatus status;
    private WalletWithdrawState currentState;
    private String failureReason;
    private String walletTransactionIdRef;
    private String paymentProviderIdRef;

    WalletWithdraw(UUID id, Long userId, BigDecimal amount, Recipient recipient) {
        this.id = id;
        this.userId = userId;
        this.recipient = recipient;
        this.amount = amount;

        this.fee = this.calculateResultingFee();

        this.status = WalletWithdrawStatus.CREATED;
        this.currentState = new PendingDebitState();
    }

    public static WalletWithdraw reconstruct(UUID id, Long userId, BigDecimal amount, Recipient recipient, WalletWithdrawStatus status, Instant createdAt, String failureReason, String walletTransactionIdRef, String paymentProviderIdRef) {
        var withdraw = new WalletWithdraw(id, userId, amount, recipient);
        withdraw.createdAt = createdAt;
        withdraw.failureReason = failureReason;
        withdraw.walletTransactionIdRef = walletTransactionIdRef;
        withdraw.paymentProviderIdRef = paymentProviderIdRef;
        withdraw.changeState(getStateFromStatus(status), status);
        return withdraw;
    }

    private static WalletWithdrawState getStateFromStatus(WalletWithdrawStatus status) {
        return switch (status) {
            case CREATED -> new PendingDebitState();
            case WALLET_DEBITED -> new WalletDebitedState();
            case COMPLETED -> new CompletedState();
            case FAILED, REFUNDED -> new FailedState();
            case COMPENSATION_PENDING -> new CompensationPendingState();
        };
    }

    public BigDecimal calculateResultingFee() {
        return amount.multiply(FeeCalculator.FEE).setScale(2, RoundingMode.HALF_UP);
    }

    private transient final List<DomainEvent> domainEvents = new ArrayList<>();

    public void registerDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> currentEvents = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return currentEvents;
    }

    public void processDebit(WalletBalancePort balancePort, WalletServicePort walletServicePort) {
        this.currentState.processDebit(this, balancePort, walletServicePort);
    }

    public void processPayment(PaymentProviderPort paymentProviderPort, PaymentSourceProviderPort paymentSourceProviderPort) {
        this.currentState.processPayment(this, paymentProviderPort, paymentSourceProviderPort);
    }

    public void changeState(WalletWithdrawState newState, WalletWithdrawStatus newStatus) {
        this.currentState = newState;
        this.status = newStatus;
    }

    public void markAsFailed(WalletWithdrawState failedState, String reason) {
        changeState(failedState, WalletWithdrawStatus.FAILED);
        this.failureReason = reason;
        registerDomainEvent(new WalletWithdrawFailedEvent(this.getId(), reason));
    }

    public void markForCompensation(String reason) {
        changeState(new CompensationPendingState(), WalletWithdrawStatus.COMPENSATION_PENDING);
        this.failureReason = reason;
        registerDomainEvent(new WalletCompensationPendingEvent(this.getId(), this.getAmount().add(this.getFee())));
    }

    public void completeCompensation() {
        changeState(new FailedState(), WalletWithdrawStatus.REFUNDED);
        registerDomainEvent(new WalletRefundCompletedEvent(this.getId()));
    }

    public void attemptCompensation(WalletServicePort walletServicePort) {
        try {
            final var refundAmount = this.getAmount().add(this.getFee());
            walletServicePort.topUp(this.getUserId(), refundAmount, this.getId());
            this.completeCompensation();
        } catch (WalletNotFoundException | WalletServiceException e) {
            this.markAsFailed(new FailedState(), "Compensation failed: " + e.getMessage());
        }
    }

    public UUID getId() {
        return this.id;
    }

    public WalletWithdrawStatus getStatus() {
        return this.status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Recipient getRecipient() {
        return this.recipient;
    }

    public Long getUserId() {
        return this.userId;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public BigDecimal getFee() {
        return this.fee;
    }

    public BigDecimal getAmountForRecipient() {
        return this.amount.subtract(this.fee);
    }

    public String getWalletTransactionIdRef() {
        return walletTransactionIdRef;
    }

    public void setWalletTransactionIdRef(String walletTransactionIdRef) {
        this.walletTransactionIdRef = walletTransactionIdRef;
    }

    public String getPaymentProviderIdRef() {
        return paymentProviderIdRef;
    }

    public void setPaymentProviderIdRef(String paymentProviderIdRef) {
        this.paymentProviderIdRef = paymentProviderIdRef;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
