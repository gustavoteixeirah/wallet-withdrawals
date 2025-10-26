package com.teixeirah.withdrawals.infrastructure;

import java.util.UUID;

import com.teixeirah.withdrawals.application.command.ProcessWalletDebitCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessWalletDebitUseCase;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.teixeirah.withdrawals.infrastructure.support.DatabaseTestUtils.resetWalletWithdrawalsTable;
import static com.teixeirah.withdrawals.infrastructure.support.RestAssuredTestSupport.configureForPort;
import static com.teixeirah.withdrawals.infrastructure.support.WalletWithdrawalTestDataBuilder.walletWithdrawal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.generated.wallet_withdrawals.Tables.WALLET_WITHDRAWALS_;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

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
        configureForPort(port);
        resetWalletWithdrawalsTable(dsl);
    }

    @Test
    void shouldRollbackDebitWhenEventPublishingFails() {
        UUID withdrawalId = UUID.randomUUID();
        walletWithdrawal().withId(withdrawalId).withStatus("CREATED").insert(dsl);
        ProcessWalletDebitCommand command = new ProcessWalletDebitCommand(withdrawalId);
        when(walletServicePort.debit(any(), any(), any())).thenReturn(12345L);
        doThrow(new RuntimeException("Simulated event publishing failure"))
                .when(domainEventPublisher).publish(any());

        assertThrows(RuntimeException.class, () -> {
            processWalletDebitUseCase.execute(command);
        }, "Simulated event publishing failure");

        String status = dsl.select(WALLET_WITHDRAWALS_.STATUS)
                .from(WALLET_WITHDRAWALS_)
                .where(WALLET_WITHDRAWALS_.ID.eq(withdrawalId))
                .fetchOneInto(String.class);

        assertThat(status).isEqualTo("CREATED");
    }
}
