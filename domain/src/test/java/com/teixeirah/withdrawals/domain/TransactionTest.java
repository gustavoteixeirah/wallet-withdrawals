package com.teixeirah.withdrawals.domain;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletWithdrawCreatedEvent;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void shouldCreateTransactionInPendingState() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        assertNotNull(walletWithdraw);
        assertNotNull(walletWithdraw.getId());
        assertEquals(WalletWithdrawStatus.CREATED, walletWithdraw.getStatus());
        assertNotNull(walletWithdraw.getCreatedAt());
    }

    @Test
    void shouldCalculateFeeCorrectlyOnCreation() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        assertEquals(new BigDecimal("10.00"), walletWithdraw.getFee());
    }

    @Test
    void shouldStoreCorrectRecipientUserIdAndAmountOnCreation() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(100.00);
        WalletWithdraw walletWithdraw = WalletWithdrawOperations.create(userId, amount, recipient);

        assertEquals(userId, walletWithdraw.getUserId());
        assertEquals(amount, walletWithdraw.getAmount());
        assertEquals(recipient, walletWithdraw.getRecipient());
    }

    @Test
    void shouldRegisterTransactionCreatedEventOnCreation() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        List<DomainEvent> events = walletWithdraw.pullDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(WalletWithdrawCreatedEvent.class, events.get(0));
        WalletWithdrawCreatedEvent event = (WalletWithdrawCreatedEvent) events.get(0);
        assertEquals(walletWithdraw.getId(), event.withdrawalId());
    }

    @Test
    void shouldClearEventsAfterPulling() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        walletWithdraw.pullDomainEvents();
        List<DomainEvent> eventsAfter = walletWithdraw.pullDomainEvents();
        assertTrue(eventsAfter.isEmpty());
    }

    @Test
    void shouldCalculateCorrectFeeForValidAmount() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        BigDecimal fee = walletWithdraw.calculateResultingFee();
        assertEquals(new BigDecimal("10.00"), fee);
    }
}
