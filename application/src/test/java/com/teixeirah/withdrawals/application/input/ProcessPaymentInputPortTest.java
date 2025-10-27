package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.ProcessPaymentCommand;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentProviderException;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentRejectedException;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawOperations;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import com.teixeirah.withdrawals.domain.wallet.withdraw.exceptions.WalletWithdrawNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.withdraw.state.WalletDebitedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessPaymentInputPortTest {

    private WalletWithdrawRepository walletWithdrawRepository;
    private PaymentProviderPort paymentProviderPort;
    private PaymentSourceProviderPort paymentSourceProviderPort;
    private DomainEventPublisherPort eventPublisher;
    private ProcessPaymentInputPort processPaymentInputPort;

    @BeforeEach
    void setUp() {
        walletWithdrawRepository = mock(WalletWithdrawRepository.class);
        paymentProviderPort = mock(PaymentProviderPort.class);
        paymentSourceProviderPort = mock(PaymentSourceProviderPort.class);
        eventPublisher = mock(DomainEventPublisherPort.class);
        processPaymentInputPort = new ProcessPaymentInputPort(
                walletWithdrawRepository,
                paymentProviderPort,
                paymentSourceProviderPort,
                eventPublisher
        );
    }

    @Test
    void shouldSuccessfullyProcessPaymentWhenWalletWithdrawExistsAndPaymentSucceeds() throws PaymentRejectedException, PaymentProviderException {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        ProcessPaymentCommand command = new ProcessPaymentCommand(withdrawalId);

        // Create a real WalletWithdraw instance and spy it
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = spy(WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient));
        when(walletWithdraw.getId()).thenReturn(withdrawalId);
        // Change state to WalletDebitedState (WALLET_DEBITED) as if debit was already processed
        walletWithdraw.changeState(new WalletDebitedState(), WalletWithdrawStatus.WALLET_DEBITED);

        when(walletWithdrawRepository.findById(withdrawalId)).thenReturn(walletWithdraw);
        when(paymentProviderPort.createPayment(any())).thenReturn("receipt123");

        // When
        processPaymentInputPort.execute(command);

        // Then
        verify(walletWithdrawRepository).findById(withdrawalId);
        verify(walletWithdraw).processPayment(any(), any());
        verify(paymentProviderPort).createPayment(any());
        verify(walletWithdrawRepository).save(walletWithdraw);
        verify(eventPublisher).publish(anyList());
    }

    @Test
    void shouldThrowExceptionWhenWalletWithdrawNotFound() {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        ProcessPaymentCommand command = new ProcessPaymentCommand(withdrawalId);

        when(walletWithdrawRepository.findById(withdrawalId))
                .thenThrow(new WalletWithdrawNotFoundException("Wallet withdraw not found"));

        // When & Then
        assertThrows(WalletWithdrawNotFoundException.class, () -> processPaymentInputPort.execute(command));

        verify(walletWithdrawRepository).findById(withdrawalId);
        verifyNoInteractions(paymentProviderPort);
        verifyNoMoreInteractions(walletWithdrawRepository);
    }

    @Test
    void shouldHandlePaymentFailureWhenPaymentRejected() throws PaymentRejectedException, PaymentProviderException {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        ProcessPaymentCommand command = new ProcessPaymentCommand(withdrawalId);

        // Create a real WalletWithdraw instance and spy it
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = spy(WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient));
        when(walletWithdraw.getId()).thenReturn(withdrawalId);
        // Change state to WalletDebitedState (WALLET_DEBITED) as if debit was already processed
        walletWithdraw.changeState(new WalletDebitedState(), WalletWithdrawStatus.WALLET_DEBITED);

        when(walletWithdrawRepository.findById(withdrawalId)).thenReturn(walletWithdraw);
        doThrow(new PaymentRejectedException("Invalid recipient account")).when(paymentProviderPort).createPayment(any());

        // When
        processPaymentInputPort.execute(command);

        // Then
        verify(walletWithdrawRepository).findById(withdrawalId);
        verify(walletWithdraw).processPayment(any(), any());
        verify(paymentProviderPort).createPayment(any());
        verify(walletWithdrawRepository).save(walletWithdraw);
        verify(eventPublisher).publish(anyList());

        // Verify the aggregate state changed to COMPENSATION_PENDING
        assertEquals(WalletWithdrawStatus.COMPENSATION_PENDING, walletWithdraw.getStatus());
    }

    @Test
    void shouldHandlePaymentFailureWhenPaymentProviderError() throws PaymentRejectedException, PaymentProviderException {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        ProcessPaymentCommand command = new ProcessPaymentCommand(withdrawalId);

        // Create a real WalletWithdraw instance and spy it
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = spy(WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient));
        when(walletWithdraw.getId()).thenReturn(withdrawalId);
        // Change state to WalletDebitedState (WALLET_DEBITED) as if debit was already processed
        walletWithdraw.changeState(new WalletDebitedState(), WalletWithdrawStatus.WALLET_DEBITED);

        when(walletWithdrawRepository.findById(withdrawalId)).thenReturn(walletWithdraw);
        doThrow(new PaymentProviderException("Payment provider timeout")).when(paymentProviderPort).createPayment(any());

        // When
        processPaymentInputPort.execute(command);

        // Then
        verify(walletWithdrawRepository).findById(withdrawalId);
        verify(walletWithdraw).processPayment(any(), any());
        verify(paymentProviderPort).createPayment(any());
        verify(walletWithdrawRepository).save(walletWithdraw);
        verify(eventPublisher).publish(anyList());

        // Verify the aggregate state changed to COMPENSATION_PENDING
        assertEquals(WalletWithdrawStatus.COMPENSATION_PENDING, walletWithdraw.getStatus());
    }

    @Test
    void shouldPropagateExceptionWhenRepositorySaveFails() throws PaymentRejectedException, PaymentProviderException {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        ProcessPaymentCommand command = new ProcessPaymentCommand(withdrawalId);

        // Create a real WalletWithdraw instance and spy it
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = spy(WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient));
        when(walletWithdraw.getId()).thenReturn(withdrawalId);
        // Change state to WalletDebitedState (WALLET_DEBITED) as if debit was already processed
        walletWithdraw.changeState(new WalletDebitedState(), WalletWithdrawStatus.WALLET_DEBITED);

        when(walletWithdrawRepository.findById(withdrawalId)).thenReturn(walletWithdraw);
        when(paymentProviderPort.createPayment(any())).thenReturn("receipt123");

        RuntimeException saveException = new RuntimeException("Database error");
        doThrow(saveException).when(walletWithdrawRepository).save(walletWithdraw);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> processPaymentInputPort.execute(command));

        assertEquals(saveException, thrown);
        verify(walletWithdrawRepository).findById(withdrawalId);
        verify(walletWithdraw).processPayment(any(), any());
        verify(paymentProviderPort).createPayment(any());
        verify(walletWithdrawRepository).save(walletWithdraw);
    }
}