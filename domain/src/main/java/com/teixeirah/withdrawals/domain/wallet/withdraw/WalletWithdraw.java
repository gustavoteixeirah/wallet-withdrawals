package com.teixeirah.withdrawals.domain.wallet.withdraw;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.state.PendingDebitState;
import com.teixeirah.withdrawals.domain.wallet.withdraw.state.WalletWithdrawState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WalletWithdraw {

    public static final double FEE = 0.10;

    private final UUID id;
    private final Recipient recipient;
    private final Long userId;
    private final BigDecimal amount;
    private final BigDecimal fee;
    private final Instant createdAt = Instant.now();

    private WalletWithdrawStatus status;
    private WalletWithdrawState currentState;

    WalletWithdraw(UUID id, Long userId, BigDecimal amount, Recipient recipient) {
        this.id = id;
        this.userId = userId;
        this.recipient = recipient;
        this.amount = amount;

        this.fee = this.calculateResultingFee();

        this.status = WalletWithdrawStatus.CREATED;
        this.currentState = new PendingDebitState();
    }

    public BigDecimal calculateResultingFee() {
        return amount.multiply(BigDecimal.valueOf(FEE)).setScale(2, RoundingMode.HALF_UP);
    }

    private transient final List<DomainEvent> domainEvents = new ArrayList<>();

    void registerDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> currentEvents = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return currentEvents;
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
}
