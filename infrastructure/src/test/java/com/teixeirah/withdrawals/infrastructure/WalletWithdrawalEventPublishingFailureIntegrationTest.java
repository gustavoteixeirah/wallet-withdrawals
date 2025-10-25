package com.teixeirah.withdrawals.infrastructure;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import org.jooq.DSLContext;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestEventPublishingFailureConfig.class)
class WalletWithdrawalEventPublishingFailureIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    @Qualifier("writeDsl")
    private DSLContext dsl;

    @LocalServerPort
    int port;

    @BeforeEach
    void setupRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        dsl.execute("SET search_path TO wallet_withdrawals");
        dsl.execute("DELETE FROM wallet_withdrawals");
    }

    @Test
    void shouldReturn500AndNotSaveWalletWithdrawalWhenEventPublishingFails() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "userId": 1,
                            "amount": 100.00,
                            "recipientFirstName": "John",
                            "recipientLastName": "Doe",
                            "recipientRoutingNumber": "123456789",
                            "recipientNationalId": "12345678901",
                            "recipientAccountNumber": "987654321"
                        }
                        """)
                .when()
                .post("/api/v1/wallet_withdraw")
                .then()
                .statusCode(500);

        Integer count = dsl.fetch("SELECT COUNT(*) FROM wallet_withdrawals").into(Integer.class).getFirst();
        assertThat(count).isEqualTo(0);
    }
}