package com.teixeirah.withdrawals.domain.wallet.withdraw.state;

import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.InsufficientFundsException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletServiceException;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingDebitStateTest {

    @Mock
    private WalletServicePort walletServicePort;

    private WalletWithdraw walletWithdraw;

    private PendingDebitState pendingDebitState;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pendingDebitState = new PendingDebitState();

        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw realWalletWithdraw = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        walletWithdraw = spy(realWalletWithdraw);
    }

    @Test
    void shouldCreatePendingState() {
        PendingDebitState state = new PendingDebitState();

        assertNotNull(state);
        assertInstanceOf(WalletWithdrawState.class, state);
    }

    @Test
    void shouldProcessDebitSuccessfully() throws InsufficientFundsException, WalletNotFoundException, WalletServiceException {
        // Given
        when(walletServicePort.debit(eq(1L), eq(new BigDecimal("110.00")), any())).thenReturn(123L);

        // When
        pendingDebitState.processDebit(walletWithdraw, walletServicePort);

        // Then
        verify(walletServicePort).debit(eq(1L), eq(new BigDecimal("110.00")), any());
        verify(walletWithdraw).changeState(any(WalletDebitedState.class), eq(WalletWithdrawStatus.WALLET_DEBITED));
        verify(walletWithdraw).registerDomainEvent(any());
        assertEquals(WalletWithdrawStatus.WALLET_DEBITED, walletWithdraw.getStatus());
    }

    @Test
    void shouldHandleInsufficientFundsException() throws InsufficientFundsException, WalletNotFoundException, WalletServiceException {
        // Given
        InsufficientFundsException exception = new InsufficientFundsException("Insufficient funds");
        when(walletServicePort.debit(anyLong(), any(BigDecimal.class), any())).thenThrow(exception);

        // When
        pendingDebitState.processDebit(walletWithdraw, walletServicePort);

        // Then
        verify(walletServicePort).debit(anyLong(), any(BigDecimal.class), any());
        verify(walletWithdraw).markAsFailed(any(FailedState.class), eq("Insufficient funds"));
        assertEquals(WalletWithdrawStatus.FAILED, walletWithdraw.getStatus());
    }

    @Test
    void shouldHandleWalletNotFoundException() throws InsufficientFundsException, WalletNotFoundException, WalletServiceException {
        // Given
        WalletNotFoundException exception = new WalletNotFoundException("Wallet not found");
        when(walletServicePort.debit(anyLong(), any(BigDecimal.class), any())).thenThrow(exception);

        // When
        pendingDebitState.processDebit(walletWithdraw, walletServicePort);

        // Then
        verify(walletServicePort).debit(anyLong(), any(BigDecimal.class), any());
        verify(walletWithdraw).markAsFailed(any(FailedState.class), eq("Wallet not found"));
        assertEquals(WalletWithdrawStatus.FAILED, walletWithdraw.getStatus());
    }

    @Test
    void shouldHandleWalletServiceException() throws InsufficientFundsException, WalletNotFoundException, WalletServiceException {
        // Given
        WalletServiceException exception = new WalletServiceException("Service unavailable");
        when(walletServicePort.debit(anyLong(), any(BigDecimal.class), any())).thenThrow(exception);

        // When
        pendingDebitState.processDebit(walletWithdraw, walletServicePort);

        // Then
        verify(walletServicePort).debit(anyLong(), any(BigDecimal.class), any());
        verify(walletWithdraw).markAsFailed(any(FailedState.class), eq("Wallet service error: Service unavailable"));
        assertEquals(WalletWithdrawStatus.FAILED, walletWithdraw.getStatus());
    }

    @Test
    void shouldHandleGenericExceptionFromWalletService() throws InsufficientFundsException, WalletNotFoundException, WalletServiceException {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error");
        when(walletServicePort.debit(anyLong(), any(BigDecimal.class), any())).thenThrow(exception);

        // When & Then
        assertThrows(RuntimeException.class, () ->
            pendingDebitState.processDebit(walletWithdraw, walletServicePort));

        // Then
        verify(walletServicePort).debit(anyLong(), any(BigDecimal.class), any());
        // Should not change state for unhandled exceptions
        assertEquals(WalletWithdrawStatus.CREATED, walletWithdraw.getStatus());
    }

    @Test
    void shouldClearEventsAfterPublishingOnDebitSuccess() throws InsufficientFundsException, WalletNotFoundException, WalletServiceException {
        // Given
        when(walletServicePort.debit(eq(1L), eq(new BigDecimal("110.00")), any())).thenReturn(123L);

        // When
        pendingDebitState.processDebit(walletWithdraw, walletServicePort);

        // Then
        var firstBatch = walletWithdraw.pullDomainEvents();
        // Created + WalletDebited events
        assertEquals(2, firstBatch.size());

        var secondBatch = walletWithdraw.pullDomainEvents();
        assertTrue(secondBatch.isEmpty());
    }
}
