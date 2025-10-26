package com.teixeirah.withdrawals.infrastructure;

import java.util.UUID;

import com.teixeirah.withdrawals.application.command.ProcessPaymentCommand;
import com.teixeirah.withdrawals.application.usecase.ProcessPaymentUseCase;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentSource;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
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
import static com.teixeirah.withdrawals.infrastructure.support.PaymentSourceTestDataBuilder.paymentSource;
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
class ProcessPaymentEventPublishingFailureIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @MockitoBean
    private DomainEventPublisherPort domainEventPublisher;

    @MockitoBean
    private PaymentProviderPort paymentProviderPort;

    @MockitoBean
    private PaymentSourceProviderPort paymentSourceProviderPort;

    @Autowired
    @Qualifier("writeDsl")
    private DSLContext dsl;

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @LocalServerPort
    int port;

    @BeforeEach
    void setup() {
        configureForPort(port);
        resetWalletWithdrawalsTable(dsl);
    }

    @Test
    void shouldRollbackPaymentWhenEventPublishingFails() {
        UUID withdrawalId = UUID.randomUUID();
        walletWithdrawal().withId(withdrawalId).withStatus("WALLET_DEBITED").insert(dsl);
        ProcessPaymentCommand command = new ProcessPaymentCommand(withdrawalId);
        when(paymentProviderPort.createPayment(any())).thenReturn("receipt-12345");
        PaymentSource source = paymentSource().build();
        when(paymentSourceProviderPort.getPaymentSource()).thenReturn(source);

        doThrow(new RuntimeException("Simulated event publishing failure"))
                .when(domainEventPublisher).publish(any());

        assertThrows(RuntimeException.class, () -> {
            processPaymentUseCase.execute(command);
        }, "Simulated event publishing failure");

        String status = dsl.select(WALLET_WITHDRAWALS_.STATUS)
                .from(WALLET_WITHDRAWALS_)
                .where(WALLET_WITHDRAWALS_.ID.eq(withdrawalId))
                .fetchOneInto(String.class);

        assertThat(status).isEqualTo("WALLET_DEBITED");
    }
}
