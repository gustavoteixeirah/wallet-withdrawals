package com.teixeirah.withdrawals.infrastructure;

import com.teixeirah.withdrawals.application.command.ProcessWalletDebitCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessWalletDebitUseCase;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import io.restassured.RestAssured;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProcessWalletDebitEventPublishingFailureIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @MockitoBean
    private DomainEventPublisherPort domainEventPublisher;

    @MockitoBean
    private WalletServicePort walletServicePort;

    @Autowired
    @Qualifier("writeDsl")
    private DSLContext dsl;

    @Autowired
    private ProcessWalletDebitUseCase processWalletDebitUseCase;

    @LocalServerPort
    int port;


    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        dsl.execute("SET search_path TO wallet_withdrawals");
        dsl.execute("DELETE FROM wallet_withdrawals");
    }

    @Test
    void shouldRollbackDebitWhenEventPublishingFails() throws Exception {
        // --- Arrange ---
        UUID withdrawalId = UUID.randomUUID();
        insertWalletWithdraw(withdrawalId, "CREATED");
        ProcessWalletDebitCommand command = new ProcessWalletDebitCommand(withdrawalId);

        // --- MOVE MOCK LOGIC HERE ---
        when(walletServicePort.debit(any(), any(), any())).thenReturn(12345L);
        doThrow(new RuntimeException("Simulated event publishing failure"))
                .when(domainEventPublisher).publish(any());

        // --- Act & Assert (Action) ---
        assertThrows(RuntimeException.class, () -> {
            processWalletDebitUseCase.execute(command);
        }, "Simulated event publishing failure");

        // --- Assert (Database) ---
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
