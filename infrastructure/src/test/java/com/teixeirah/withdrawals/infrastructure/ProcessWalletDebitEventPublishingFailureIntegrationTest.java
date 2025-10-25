package com.teixeirah.withdrawals.infrastructure;

import com.teixeirah.withdrawals.application.command.ProcessWalletDebitCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessWalletDebitUseCase;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.InsufficientFundsException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletServiceException;
import io.restassured.RestAssured;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.jooq.generated.wallet_withdrawals.Tables.WALLET_WITHDRAWALS_;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ProcessWalletDebitEventPublishingFailureIntegrationTest.Config.class)
class ProcessWalletDebitEventPublishingFailureIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    @Qualifier("writeDsl")
    private DSLContext dsl;

    @Autowired
    private ProcessWalletDebitUseCase processWalletDebitUseCase;

    @LocalServerPort
    int port;

    // This nested class provides the mock beans
    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        public DomainEventPublisherPort failingDomainEventPublisher() {
            return events -> {
                throw new RuntimeException("Simulated event publishing failure");
            };
        }

        @Bean
        @Primary
        public WalletServicePort mockWalletServicePort() throws InsufficientFundsException, WalletNotFoundException, WalletServiceException {
            // Mock the wallet service to return a successful debit
            WalletServicePort mock = mock(WalletServicePort.class);
            when(mock.debit(any(), any(), any())).thenReturn(12345L);
            return mock;
        }
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        dsl.execute("SET search_path TO wallet_withdrawals");
        dsl.execute("DELETE FROM wallet_withdrawals");
    }

    @Test
    void shouldRollbackDebitWhenEventPublishingFails() {
        // --- Arrange ---
        // Manually insert a WalletWithdraw in the 'CREATED' state
        UUID withdrawalId = UUID.randomUUID();
        insertWalletWithdraw(withdrawalId, "CREATED");

        ProcessWalletDebitCommand command = new ProcessWalletDebitCommand(withdrawalId);

        // --- Act & Assert (Action) ---
        // Execute the use case directly and assert that it throws the simulated exception
        assertThrows(RuntimeException.class, () -> {
            processWalletDebitUseCase.execute(command);
        }, "Simulated event publishing failure");

        // --- Assert (Database) ---
        // Verify that the transaction was rolled back
        // The status should still be 'CREATED', not 'WALLET_DEBITED' or 'FAILED'
        String status = dsl.select(WALLET_WITHDRAWALS_.STATUS)
                .from(WALLET_WITHDRAWALS_)
                .where(WALLET_WITHDRAWALS_.ID.eq(withdrawalId))
                .fetchOneInto(String.class);

        assertThat(status).isEqualTo("CREATED");
    }

    private void insertWalletWithdraw(UUID id, String status) {
        dsl.insertInto(WALLET_WITHDRAWALS_)
                .set(WALLET_WITHDRAWALS_.ID, id)
                .set(WALLET_WITHDRAWALS_.USER_ID, 1L)
                .set(WALLET_WITHDRAWALS_.STATUS, status)
                .set(WALLET_WITHDRAWALS_.AMOUNT, new BigDecimal("100.00"))
                .set(WALLET_WITHDRAWALS_.FEE, new BigDecimal("10.00"))
                .set(WALLET_WITHDRAWALS_.AMOUNT_FOR_RECIPIENT, new BigDecimal("90.00"))
                .set(WALLET_WITHDRAWALS_.CREATED_AT, OffsetDateTime.now())
                .set(WALLET_WITHDRAWALS_.UPDATED_AT, OffsetDateTime.now())
                .set(WALLET_WITHDRAWALS_.RECIPIENT_FIRST_NAME, "Test")
                .set(WALLET_WITHDRAWALS_.RECIPIENT_LAST_NAME, "User")
                .set(WALLET_WITHDRAWALS_.RECIPIENT_NATIONAL_ID, "12345678901")
                .set(WALLET_WITHDRAWALS_.RECIPIENT_ACCOUNT_NUMBER, "987654321")
                .set(WALLET_WITHDRAWALS_.RECIPIENT_ROUTING_NUMBER, "123456789")
                .execute();
    }
}