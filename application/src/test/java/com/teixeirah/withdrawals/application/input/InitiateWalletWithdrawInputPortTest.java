package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.InitiateWalletWithdrawalCommand;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitiateWalletWithdrawInputPortTest {

    private WalletWithdrawRepository walletWithdrawRepository;
    private DomainEventPublisherPort eventPublisher;
    private InitiateWalletWithdrawInputPort handler;

    @BeforeEach
    void setUp() {
        walletWithdrawRepository = mock(WalletWithdrawRepository.class);
        eventPublisher = mock(DomainEventPublisherPort.class);
        handler = new InitiateWalletWithdrawInputPort(walletWithdrawRepository, eventPublisher);
    }

    @Test
    void shouldCreateWalletWithdrawSaveAndPublishEventOnSuccess() {
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

        // When
        var response = handler.execute(command);

        // Then
        assertNotNull(response);
        assertNotNull(response.transactionId());
        assertEquals(WalletWithdrawStatus.CREATED, response.status());
        assertNotNull(response.createdAt());

        verify(walletWithdrawRepository).save(any(WalletWithdraw.class));
        verify(eventPublisher).publish(anyList());
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

        doThrow(new RuntimeException("Database error")).when(walletWithdrawRepository).save(any(WalletWithdraw.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> handler.execute(command));
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

        verifyNoInteractions(walletWithdrawRepository);
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

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> handler.execute(command));

        verifyNoInteractions(walletWithdrawRepository);
    }
}
