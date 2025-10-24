package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.InitiateWalletWithdrawalCommand;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawCreatedEvent;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;
import com.teixeirah.withdrawals.application.output.WalletWithdrawOutputPort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitiateWalletWithdrawInputPortTest {

    private WalletWithdrawOutputPort walletWithdrawOutputPort;
    private DomainEventPublisherPort domainEventPublisher;
    private WalletWithdrawOperations walletWithdrawOperations;
    private InitiateWalletWithdrawInputPort handler;

    @BeforeEach
    void setUp() {
        walletWithdrawOutputPort = mock(WalletWithdrawOutputPort.class);
        domainEventPublisher = mock(DomainEventPublisherPort.class);
        walletWithdrawOperations = mock(WalletWithdrawOperations.class);
        handler = new InitiateWalletWithdrawInputPort(walletWithdrawOutputPort, domainEventPublisher, walletWithdrawOperations);
    }

    @Test
    void shouldCreateTransactionSaveAndPublishEventOnSuccess() {
        // Given
        var command = new InitiateWalletWithdrawalCommand(
                1L,
                BigDecimal.valueOf(100.00),
                "John",
                "Doe",
                "123456789",
                "123456789",
                "987654321"
        );

        var account = new Account("987654321", "123456789");
        var recipient = new Recipient("John", "Doe", "123456789", account);
        var mockTransaction = mock(WalletWithdraw.class);
        var transactionId = UUID.randomUUID();
        when(mockTransaction.getId()).thenReturn(transactionId);
        when(mockTransaction.getStatus()).thenReturn(WalletWithdrawStatus.CREATED);
        when(mockTransaction.getCreatedAt()).thenReturn(java.time.Instant.now());
        when(mockTransaction.getUserId()).thenReturn(1L);
        when(mockTransaction.getAmount()).thenReturn(BigDecimal.valueOf(100.00));
        when(mockTransaction.getRecipient()).thenReturn(recipient);
        when(mockTransaction.pullDomainEvents()).thenReturn(List.of(mock(WalletWithdrawCreatedEvent.class)));

        when(walletWithdrawOperations.create(any(Long.class), any(BigDecimal.class), any(Recipient.class))).thenReturn(mockTransaction);
        // save is void, no return

        // When
        var response = handler.execute(command);

        // Then
        assertNotNull(response);
        assertEquals(transactionId, response.transactionId());
        assertEquals(WalletWithdrawStatus.CREATED, response.status());
        assertNotNull(response.createdAt());

        verify(walletWithdrawOutputPort).save(mockTransaction);
        verify(domainEventPublisher).publish(any());
    }

    @Test
    void shouldPropagateExceptionAndNotPublishWhenRepositorySaveFails() {
        // Given
        var command = new InitiateWalletWithdrawalCommand(
                1L,
                BigDecimal.valueOf(100.00),
                "John",
                "Doe",
                "123456789",
                "123456789",
                "987654321"
        );

        var mockTransaction = mock(WalletWithdraw.class);
        when(walletWithdrawOperations.create(any(), any(), any())).thenReturn(mockTransaction);
        doThrow(new RuntimeException("Database error")).when(walletWithdrawOutputPort).save(mockTransaction);

        // When & Then
        assertThrows(RuntimeException.class, () -> handler.execute(command));

        verify(domainEventPublisher, never()).publish(any());
    }

    @Test
    void shouldPropagateExceptionWhenPublisherFailsAfterSave() {
        // Given
        var command = new InitiateWalletWithdrawalCommand(
                1L,
                BigDecimal.valueOf(100.00),
                "John",
                "Doe",
                "123456789",
                "123456789",
                "987654321"
        );

        var mockTransaction = mock(WalletWithdraw.class);
        when(walletWithdrawOperations.create(any(), any(), any())).thenReturn(mockTransaction);
        doThrow(new RuntimeException("Publisher error")).when(domainEventPublisher).publish(any());

        // When & Then
        assertThrows(RuntimeException.class, () -> handler.execute(command));

        verify(walletWithdrawOutputPort).save(mockTransaction);
    }

    @Test
    void shouldFailWhenCommandRecipientIsInvalid() {
        // Given
        var command = new InitiateWalletWithdrawalCommand(
                1L,
                BigDecimal.valueOf(100.00),
                null, // invalid firstName
                "Doe",
                "123456789",
                "123456789",
                "987654321"
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> handler.execute(command));

        verifyNoInteractions(walletWithdrawOutputPort);
        verifyNoInteractions(domainEventPublisher);
    }

    @Test
    void shouldFailWhenCommandAmountIsInvalid() {
        // Given
        var command = new InitiateWalletWithdrawalCommand(
                1L,
                BigDecimal.ZERO, // invalid amount
                "John",
                "Doe",
                "123456789",
                "123456789",
                "987654321"
        );

        when(walletWithdrawOperations.create(any(), eq(BigDecimal.ZERO), any())).thenThrow(new IllegalArgumentException());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> handler.execute(command));

        verifyNoInteractions(walletWithdrawOutputPort);
        verifyNoInteractions(domainEventPublisher);
    }
}
