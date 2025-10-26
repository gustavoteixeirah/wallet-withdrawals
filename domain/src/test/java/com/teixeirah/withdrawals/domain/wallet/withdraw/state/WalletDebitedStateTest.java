package com.teixeirah.withdrawals.domain.wallet.withdraw.state;

import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentRequest;
import com.teixeirah.withdrawals.domain.payments.PaymentSource;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceInformation;
import com.teixeirah.withdrawals.domain.payments.PaymentAccount;
import com.teixeirah.withdrawals.domain.payments.PaymentDestination;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentProviderException;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentRejectedException;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletDebitedStateTest {

    @Mock
    private PaymentProviderPort paymentProviderPort;

    @Mock
    private PaymentSourceProviderPort paymentSourceProviderPort;

    private WalletWithdraw walletWithdraw;

    private WalletDebitedState walletDebitedState;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        walletDebitedState = new WalletDebitedState();

        // Create a real WalletWithdraw instance and spy it
        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw realWalletWithdraw = WalletWithdrawOperations.create(1L, BigDecimal.valueOf(100.00), recipient);

        // Spy the real instance
        walletWithdraw = spy(realWalletWithdraw);

        // Put it in WALLET_DEBITED state (WalletDebitedState)
        walletWithdraw.changeState(new WalletDebitedState(), WalletWithdrawStatus.WALLET_DEBITED);
    }

    private PaymentRequest createExpectedPaymentRequest() {
        PaymentSource source = new PaymentSource(
                "COMPANY",
                new PaymentSourceInformation("ONTOP INC"),
                new PaymentAccount("0245253419", "USD", "028444018")
        );

        String recipientName = walletWithdraw.getRecipient().firstName() + " " + walletWithdraw.getRecipient().lastName();
        PaymentDestination destination = new PaymentDestination(
                recipientName,
                new PaymentAccount(
                        walletWithdraw.getRecipient().account().accountNumber(),
                        "USD",
                        walletWithdraw.getRecipient().account().routingNumber()
                )
        );

        return new PaymentRequest(source, destination, walletWithdraw.getAmountForRecipient());
    }

    @Test
    void shouldCreateWalletDebitedState() {
        WalletDebitedState state = new WalletDebitedState();

        assertNotNull(state);
        assertInstanceOf(WalletWithdrawState.class, state);
    }

    @Test
    void shouldProcessPaymentSuccessfully() throws PaymentRejectedException, PaymentProviderException {
        // Given
        PaymentSource expectedSource = new PaymentSource(
                "COMPANY",
                new PaymentSourceInformation("ONTOP INC"),
                new PaymentAccount("0245253419", "USD", "028444018")
        );
        when(paymentSourceProviderPort.getPaymentSource()).thenReturn(expectedSource);
        PaymentRequest expectedPaymentRequest = createExpectedPaymentRequest();
        when(paymentProviderPort.createPayment(expectedPaymentRequest)).thenReturn("receipt123");

        // When
        walletDebitedState.processPayment(walletWithdraw, paymentProviderPort, paymentSourceProviderPort);

        // Then
        verify(paymentProviderPort).createPayment(expectedPaymentRequest);
        verify(walletWithdraw).changeState(any(CompletedState.class), eq(WalletWithdrawStatus.COMPLETED));
        verify(walletWithdraw).registerDomainEvent(any());
        assertEquals(WalletWithdrawStatus.COMPLETED, walletWithdraw.getStatus());
    }

    @Test
    void shouldHandlePaymentRejectedException() throws PaymentRejectedException, PaymentProviderException {
        // Given
        PaymentSource expectedSource = new PaymentSource(
                "COMPANY",
                new PaymentSourceInformation("ONTOP INC"),
                new PaymentAccount("0245253419", "USD", "028444018")
        );
        when(paymentSourceProviderPort.getPaymentSource()).thenReturn(expectedSource);
        PaymentRequest expectedPaymentRequest = createExpectedPaymentRequest();
        PaymentRejectedException exception = new PaymentRejectedException("Invalid recipient account");
        when(paymentProviderPort.createPayment(expectedPaymentRequest)).thenThrow(exception);

        // When
        walletDebitedState.processPayment(walletWithdraw, paymentProviderPort, paymentSourceProviderPort);

        // Then
        verify(paymentProviderPort).createPayment(expectedPaymentRequest);
        verify(walletWithdraw).markAsFailed(any(FailedState.class), eq("Payment rejected: Invalid recipient account"));
        assertEquals(WalletWithdrawStatus.FAILED, walletWithdraw.getStatus());
    }

    @Test
    void shouldHandlePaymentProviderException() throws PaymentRejectedException, PaymentProviderException {
        // Given
        PaymentSource expectedSource = new PaymentSource(
                "COMPANY",
                new PaymentSourceInformation("ONTOP INC"),
                new PaymentAccount("0245253419", "USD", "028444018")
        );
        when(paymentSourceProviderPort.getPaymentSource()).thenReturn(expectedSource);
        PaymentRequest expectedPaymentRequest = createExpectedPaymentRequest();
        PaymentProviderException exception = new PaymentProviderException("Payment provider timeout");
        when(paymentProviderPort.createPayment(expectedPaymentRequest)).thenThrow(exception);

        // When
        walletDebitedState.processPayment(walletWithdraw, paymentProviderPort, paymentSourceProviderPort);

        // Then
        verify(paymentProviderPort).createPayment(expectedPaymentRequest);
        verify(walletWithdraw).markAsFailed(any(FailedState.class), eq("Payment provider error: Payment provider timeout"));
        assertEquals(WalletWithdrawStatus.FAILED, walletWithdraw.getStatus());
    }

    @Test
    void shouldHandleGenericExceptionFromPaymentProvider() throws PaymentRejectedException, PaymentProviderException {
        // Given
        PaymentSource expectedSource = new PaymentSource(
                "COMPANY",
                new PaymentSourceInformation("ONTOP INC"),
                new PaymentAccount("0245253419", "USD", "028444018")
        );
        when(paymentSourceProviderPort.getPaymentSource()).thenReturn(expectedSource);
        PaymentRequest expectedPaymentRequest = createExpectedPaymentRequest();
        RuntimeException exception = new RuntimeException("Unexpected error");
        when(paymentProviderPort.createPayment(expectedPaymentRequest)).thenThrow(exception);

        // When & Then
        assertThrows(RuntimeException.class, () ->
            walletDebitedState.processPayment(walletWithdraw, paymentProviderPort, paymentSourceProviderPort));

        // Then
        verify(paymentProviderPort).createPayment(expectedPaymentRequest);
        // Should not change state for unhandled exceptions
        assertEquals(WalletWithdrawStatus.WALLET_DEBITED, walletWithdraw.getStatus());
    }

    @Test
    void shouldClearEventsAfterPublishingOnPaymentSuccess() throws PaymentRejectedException, PaymentProviderException {
        // Given
        PaymentSource expectedSource = new PaymentSource(
                "COMPANY",
                new PaymentSourceInformation("ONTOP INC"),
                new PaymentAccount("0245253419", "USD", "028444018")
        );
        when(paymentSourceProviderPort.getPaymentSource()).thenReturn(expectedSource);
        when(paymentProviderPort.createPayment(any())).thenReturn("receipt123");

        // When
        walletDebitedState.processPayment(walletWithdraw, paymentProviderPort, paymentSourceProviderPort);

        // Then
        var firstBatch = walletWithdraw.pullDomainEvents();
        // Created + WithdrawalCompleted events
        assertEquals(2, firstBatch.size());

        var secondBatch = walletWithdraw.pullDomainEvents();
        assertTrue(secondBatch.isEmpty());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenProcessDebitIsCalled() {
        // Given
        // WalletDebitedState does not override processDebit, so it should throw IllegalStateException

        // When & Then
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> walletDebitedState.processDebit(walletWithdraw, null, null)
    );

        assertEquals("Cannot process debit in state WalletDebitedState", exception.getMessage());
    }
}