package com.teixeirah.withdrawals.infrastructure;

import io.restassured.http.ContentType;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.teixeirah.withdrawals.infrastructure.support.DatabaseTestUtils.resetWalletWithdrawalsTable;
import static com.teixeirah.withdrawals.infrastructure.support.RestAssuredTestSupport.configureForPort;
import static com.teixeirah.withdrawals.infrastructure.support.WalletWithdrawalRequestBuilder.walletWithdrawalRequest;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.generated.wallet_withdrawals.Tables.WALLET_WITHDRAWALS_;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.main.allow-bean-definition-overriding=true")
@Import(TestEventPublishingFailureConfig.class)
class WalletWithdrawalEventPublishingFailureIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    @Qualifier("writeDsl")
    private DSLContext dsl;

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
    }

    @BeforeEach
    void setupRestAssured() {
        configureForPort(port);
        resetWalletWithdrawalsTable(dsl);
    }

    @Test
    void shouldReturn500AndNotSaveWalletWithdrawalWhenEventPublishingFails() {
        given()
                .contentType(ContentType.JSON)
                .body(walletWithdrawalRequest().build())
                .when()
                .post("/api/v1/wallet_withdraw")
                .then()
                .statusCode(500);

        int count = dsl.fetchCount(WALLET_WITHDRAWALS_);
        assertThat(count).isEqualTo(0);
    }
}