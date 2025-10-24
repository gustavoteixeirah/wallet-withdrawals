package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.ProcessWalletDebitCommand;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.InsufficientFundsException;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import com.teixeirah.withdrawals.domain.wallet.withdraw.exceptions.WalletWithdrawNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProcessWalletDebitInputPortTest {

    private WalletWithdrawRepository walletWithdrawRepository;
    private WalletServicePort walletServicePort;
    private ProcessWalletDebitInputPort processWalletDebitInputPort;

    @BeforeEach
    void setUp() {
        walletWithdrawRepository = mock(WalletWithdrawRepository.class);
        walletServicePort = mock(WalletServicePort.class);
        processWalletDebitInputPort = new ProcessWalletDebitInputPort(
                walletWithdrawRepository,
                walletServicePort
        );
    }

    @Test
    void shouldSuccessfullyProcessDebitWhenWalletWithdrawExistsAndDebitSucceeds() {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        ProcessWalletDebitCommand command = new ProcessWalletDebitCommand(withdrawalId);

        // Create a real WalletWithdraw instance and spy it
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = spy(WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient));
        when(walletWithdraw.getId()).thenReturn(withdrawalId);

        when(walletWithdrawRepository.findById(withdrawalId)).thenReturn(walletWithdraw);

        // When
        processWalletDebitInputPort.execute(command);

        // Then
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(walletWithdrawRepository).findById(withdrawalId);
        verify(walletWithdraw).processDebit(walletServicePort);
        verify(walletServicePort).debit(eq(1L), amountCaptor.capture(), eq(withdrawalId));
        assertEquals(new BigDecimal("110.00"), amountCaptor.getValue());
        verify(walletWithdrawRepository).save(walletWithdraw);
    }

    @Test
    void shouldThrowExceptionWhenWalletWithdrawNotFound() {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        ProcessWalletDebitCommand command = new ProcessWalletDebitCommand(withdrawalId);

        when(walletWithdrawRepository.findById(withdrawalId))
                .thenThrow(new WalletWithdrawNotFoundException("Wallet withdraw not found"));

        // When & Then
        assertThrows(WalletWithdrawNotFoundException.class, () -> processWalletDebitInputPort.execute(command));

        verify(walletWithdrawRepository).findById(withdrawalId);
        verifyNoInteractions(walletServicePort);
        verifyNoMoreInteractions(walletWithdrawRepository);
    }

    @Test
    void shouldHandleDebitFailureWhenInsufficientFunds() {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        ProcessWalletDebitCommand command = new ProcessWalletDebitCommand(withdrawalId);

        // Create a real WalletWithdraw instance and spy it
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = spy(WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient));
        when(walletWithdraw.getId()).thenReturn(withdrawalId);

        when(walletWithdrawRepository.findById(withdrawalId)).thenReturn(walletWithdraw);
        doThrow(new InsufficientFundsException("Insufficient funds")).when(walletServicePort).debit(eq(1L), any(BigDecimal.class), eq(withdrawalId));

        // When
        processWalletDebitInputPort.execute(command);

        // Then
        verify(walletWithdrawRepository).findById(withdrawalId);
        verify(walletWithdraw).processDebit(walletServicePort);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(walletServicePort).debit(eq(1L), amountCaptor.capture(), eq(withdrawalId));
        assertEquals(new BigDecimal("110.00"), amountCaptor.getValue());
        verify(walletWithdrawRepository).save(walletWithdraw);

        // Verify the aggregate state changed to FAILED
        assertEquals(WalletWithdrawStatus.FAILED, walletWithdraw.getStatus());
    }

    @Test
    void shouldPropagateExceptionWhenRepositorySaveFails() {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        ProcessWalletDebitCommand command = new ProcessWalletDebitCommand(withdrawalId);

        // Create a real WalletWithdraw instance and spy it
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = spy(WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient));
        when(walletWithdraw.getId()).thenReturn(withdrawalId);

        when(walletWithdrawRepository.findById(withdrawalId)).thenReturn(walletWithdraw);

        RuntimeException saveException = new RuntimeException("Database error");
        doThrow(saveException).when(walletWithdrawRepository).save(walletWithdraw);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> processWalletDebitInputPort.execute(command));

        assertEquals(saveException, thrown);
        verify(walletWithdrawRepository).findById(withdrawalId);
        verify(walletWithdraw).processDebit(walletServicePort);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(walletServicePort).debit(eq(1L), amountCaptor.capture(), eq(withdrawalId));
        assertEquals(new BigDecimal("110.00"), amountCaptor.getValue());
        verify(walletWithdrawRepository).save(walletWithdraw);
    }

}