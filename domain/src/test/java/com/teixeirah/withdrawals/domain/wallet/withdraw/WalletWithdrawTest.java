package com.teixeirah.withdrawals.domain.wallet.withdraw;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WalletWithdrawCreatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletWithdrawTest {

    @Test
    void shouldCreateTransactionInPendingState() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw transaction = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        assertNotNull(transaction);
        assertNotNull(transaction.getId());
        assertEquals(WalletWithdrawStatus.CREATED, transaction.getStatus());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void shouldCalculateFeeCorrectlyOnCreation() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw transaction = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        assertEquals(new BigDecimal("10.00"), transaction.getFee());
    }

    @Test
    void shouldStoreCorrectRecipientUserIdAndAmountOnCreation() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(100.00);
        WalletWithdraw transaction = WalletWithdrawOperations.create(userId, amount, recipient);

        assertEquals(userId, transaction.getUserId());
        assertEquals(amount, transaction.getAmount());
        assertEquals(recipient, transaction.getRecipient());
    }

    @Test
    void shouldRegisterTransactionCreatedEventOnCreation() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw transaction = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        List<DomainEvent> events = transaction.pullDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(WalletWithdrawCreatedEvent.class, events.get(0));
        WalletWithdrawCreatedEvent event = (WalletWithdrawCreatedEvent) events.get(0);
        assertEquals(transaction, event.walletWithdraw());
    }

    @Test
    void shouldClearEventsAfterPulling() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw transaction = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        transaction.pullDomainEvents();
        List<DomainEvent> eventsAfter = transaction.pullDomainEvents();
        assertTrue(eventsAfter.isEmpty());
    }

    @Test
    void shouldCalculateCorrectFeeForValidAmount() {
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw transaction = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        BigDecimal fee = transaction.calculateResultingFee();
        assertEquals(new BigDecimal("10.00"), fee);
    }

    @Test
    void shouldReconstructWalletWithdrawInCreatedState() {
        // Given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        Instant createdAt = Instant.parse("2023-01-01T10:00:00Z");

        // When
        WalletWithdraw reconstructed = WalletWithdraw.reconstruct(
                id, userId, amount, recipient, WalletWithdrawStatus.CREATED, createdAt,
                null, null, null
        );

        // Then
        assertEquals(id, reconstructed.getId());
        assertEquals(userId, reconstructed.getUserId());
        assertEquals(amount, reconstructed.getAmount());
        assertEquals(recipient, reconstructed.getRecipient());
        assertEquals(WalletWithdrawStatus.CREATED, reconstructed.getStatus());
        assertEquals(createdAt, reconstructed.getCreatedAt());
        assertNull(reconstructed.getFailureReason());
        assertNull(reconstructed.getWalletTransactionIdRef());
        assertNull(reconstructed.getPaymentProviderIdRef());

        // Verify state behavior - should allow processDebit (no exception)
        // We can't easily test this without mocking, but the state should be PendingDebitState
    }

    @Test
    void shouldReconstructWalletWithdrawInWalletDebitedState() {
        // Given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        Instant createdAt = Instant.parse("2023-01-01T10:00:00Z");
        String walletTransactionIdRef = "wallet-tx-123";

        // When
        WalletWithdraw reconstructed = WalletWithdraw.reconstruct(
                id, userId, amount, recipient, WalletWithdrawStatus.WALLET_DEBITED, createdAt,
                null, walletTransactionIdRef, null
        );

        // Then
        assertEquals(id, reconstructed.getId());
        assertEquals(WalletWithdrawStatus.WALLET_DEBITED, reconstructed.getStatus());
        assertEquals(walletTransactionIdRef, reconstructed.getWalletTransactionIdRef());

        // Verify state behavior - processDebit should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> reconstructed.processDebit(null));
    }

    @Test
    void shouldReconstructWalletWithdrawInCompletedState() {
        // Given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        Instant createdAt = Instant.parse("2023-01-01T10:00:00Z");
        String walletTransactionIdRef = "wallet-tx-123";
        String paymentProviderIdRef = "payment-ref-456";

        // When
        WalletWithdraw reconstructed = WalletWithdraw.reconstruct(
                id, userId, amount, recipient, WalletWithdrawStatus.COMPLETED, createdAt,
                null, walletTransactionIdRef, paymentProviderIdRef
        );

        // Then
        assertEquals(id, reconstructed.getId());
        assertEquals(WalletWithdrawStatus.COMPLETED, reconstructed.getStatus());
        assertEquals(walletTransactionIdRef, reconstructed.getWalletTransactionIdRef());
        assertEquals(paymentProviderIdRef, reconstructed.getPaymentProviderIdRef());

        // Verify state behavior - both processDebit and processPayment should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> reconstructed.processDebit(null));
        assertThrows(IllegalStateException.class, () -> reconstructed.processPayment(null, null));
    }

    @Test
    void shouldReconstructWalletWithdrawInFailedState() {
        // Given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        Instant createdAt = Instant.parse("2023-01-01T10:00:00Z");
        String failureReason = "Payment rejected";

        // When
        WalletWithdraw reconstructed = WalletWithdraw.reconstruct(
                id, userId, amount, recipient, WalletWithdrawStatus.FAILED, createdAt,
                failureReason, null, null
        );

        // Then
        assertEquals(id, reconstructed.getId());
        assertEquals(WalletWithdrawStatus.FAILED, reconstructed.getStatus());
        assertEquals(failureReason, reconstructed.getFailureReason());

        // Verify state behavior - both processDebit and processPayment should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> reconstructed.processDebit(null));
        assertThrows(IllegalStateException.class, () -> reconstructed.processPayment(null, null));
    }
}
