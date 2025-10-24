package com.teixeirah.withdrawals.domain;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawCreatedEvent;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WalletWithdrawTest {

    @Test
    void shouldCreateTransactionInPendingState() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw transaction = operations.create(1L, BigDecimal.valueOf(100.00), recipient);

        assertNotNull(transaction);
        assertNotNull(transaction.getId());
        assertEquals(WalletWithdrawStatus.CREATED, transaction.getStatus());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void shouldCalculateFeeCorrectlyOnCreation() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw transaction = operations.create(1L, BigDecimal.valueOf(100.00), recipient);

        assertEquals(new BigDecimal("10.00"), transaction.getFee());
    }

    @Test
    void shouldStoreCorrectRecipientUserIdAndAmountOnCreation() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(100.00);
        WalletWithdraw transaction = operations.create(userId, amount, recipient);

        assertEquals(userId, transaction.getUserId());
        assertEquals(amount, transaction.getAmount());
        assertEquals(recipient, transaction.getRecipient());
    }

    @Test
    void shouldRegisterTransactionCreatedEventOnCreation() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw transaction = operations.create(1L, BigDecimal.valueOf(100.00), recipient);

        List<DomainEvent> events = transaction.pullDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(WalletWithdrawCreatedEvent.class, events.get(0));
        WalletWithdrawCreatedEvent event = (WalletWithdrawCreatedEvent) events.get(0);
        assertEquals(transaction, event.walletWithdraw());
    }

    @Test
    void shouldClearEventsAfterPulling() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw transaction = operations.create(1L, BigDecimal.valueOf(100.00), recipient);

        transaction.pullDomainEvents();
        List<DomainEvent> eventsAfter = transaction.pullDomainEvents();
        assertTrue(eventsAfter.isEmpty());
    }

    @Test
    void shouldCalculateCorrectFeeForValidAmount() {
        WalletWithdrawOperations operations = new WalletWithdrawOperations();
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw transaction = operations.create(1L, BigDecimal.valueOf(100.00), recipient);

        BigDecimal fee = transaction.calculateResultingFee();
        assertEquals(new BigDecimal("10.00"), fee);
    }
}
