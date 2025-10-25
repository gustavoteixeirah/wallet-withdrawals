package com.teixeirah.withdrawals.application.input;

import com.teixeirah.withdrawals.application.command.GetWalletWithdrawCommand;
import com.teixeirah.withdrawals.application.response.WalletWithdrawResponse;
import com.teixeirah.withdrawals.domain.value.objects.Account;
import com.teixeirah.withdrawals.domain.value.objects.Recipient;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import com.teixeirah.withdrawals.domain.wallet.withdraw.exceptions.WalletWithdrawNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetWalletWithdrawInputPortTest {

    private WalletWithdrawRepository walletWithdrawRepository;
    private GetWalletWithdrawInputPort getWalletWithdrawInputPort;

    @BeforeEach
    void setUp() {
        walletWithdrawRepository = mock(WalletWithdrawRepository.class);
        getWalletWithdrawInputPort = new GetWalletWithdrawInputPort(walletWithdrawRepository);
    }

    @Test
    void shouldReturnWalletWithdrawResponseWhenWalletWithdrawExists() {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        GetWalletWithdrawCommand command = new GetWalletWithdrawCommand(withdrawalId);

        Account account = new Account("987654321", "123456789");
        Recipient recipient = new Recipient("John", "Doe", "123456789", account);
        WalletWithdraw walletWithdraw = WalletWithdraw.reconstruct(
                withdrawalId,
                1L,
                new BigDecimal("100.00"),
                recipient,
                WalletWithdrawStatus.CREATED,
                Instant.parse("2023-01-01T10:00:00Z"),
                null,
                "wallet-tx-123",
                "payment-ref-456"
        );

        when(walletWithdrawRepository.findById(withdrawalId)).thenReturn(walletWithdraw);

        // When
        WalletWithdrawResponse response = getWalletWithdrawInputPort.execute(command);

        // Then
        verify(walletWithdrawRepository).findById(withdrawalId);
        assertNotNull(response);
        assertEquals(withdrawalId, response.id());
        assertEquals(1L, response.userId());
        assertEquals(new BigDecimal("100.00"), response.amount());
        assertEquals(new BigDecimal("10.00"), response.fee()); // 10% fee
        assertEquals(new BigDecimal("90.00"), response.amountForRecipient());
        assertEquals(WalletWithdrawStatus.CREATED, response.status());
        assertEquals(Instant.parse("2023-01-01T10:00:00Z"), response.createdAt());
        assertNull(response.failureReason());
        assertEquals("wallet-tx-123", response.walletTransactionIdRef());
        assertEquals("payment-ref-456", response.paymentProviderIdRef());
        assertEquals("John", response.recipientFirstName());
        assertEquals("Doe", response.recipientLastName());
        assertEquals("123456789", response.recipientNationalId());
        assertEquals("987654321", response.recipientAccountNumber());
        assertEquals("123456789", response.recipientRoutingNumber());
    }

    @Test
    void shouldThrowWalletWithdrawNotFoundExceptionWhenWalletWithdrawDoesNotExist() {
        // Given
        UUID withdrawalId = UUID.randomUUID();
        GetWalletWithdrawCommand command = new GetWalletWithdrawCommand(withdrawalId);

        when(walletWithdrawRepository.findById(withdrawalId))
                .thenThrow(new WalletWithdrawNotFoundException("Wallet withdraw not found"));

        // When & Then
        WalletWithdrawNotFoundException exception = assertThrows(
                WalletWithdrawNotFoundException.class,
                () -> getWalletWithdrawInputPort.execute(command)
        );

        assertEquals("Wallet withdraw not found", exception.getMessage());
        verify(walletWithdrawRepository).findById(withdrawalId);
    }
}