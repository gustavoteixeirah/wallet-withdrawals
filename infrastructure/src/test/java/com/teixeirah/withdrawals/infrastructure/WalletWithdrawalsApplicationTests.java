package com.teixeirah.withdrawals.infrastructure;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletWithdrawalsApplicationTests {


    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    int port;

    @BeforeEach
    void setupRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }


    @Test
    void contextLoads() {
    }

    @Test
    void shouldCreateAndProcessWalletWithdrawalSagaSuccessfully() {
        // --- 1. Create Withdrawal (POST) ---
        JsonPath jsonPath = given()
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
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("transactionId", notNullValue())
                .body("status", equalTo("CREATED"))
                .body("createdAt", notNullValue())
                .extract().jsonPath();

        String transactionId = jsonPath.getString("transactionId");

        // --- 2. Wait for Saga to reach WALLET_DEBITED ---
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            given()
                    .when()
                    .get("/api/v1/wallet_withdraw/{id}", transactionId)
                    .then()
                    .statusCode(200)
                    .body("status", anyOf(equalTo("WALLET_DEBITED"), equalTo("COMPLETED"))); // It might be fast
        });

        // --- 3. Wait for Saga to reach COMPLETED ---
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            given()
                    .when()
                    .get("/api/v1/wallet_withdraw/{id}", transactionId)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("COMPLETED"));
        });

        // --- 4. Verify Final State (GET) ---
        given()
                .when()
                .get("/api/v1/wallet_withdraw/{id}", transactionId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(transactionId))
                .body("userId", equalTo(1))
                .body("amount", is(100.00f)) // JSON floats
                .body("fee", is(10.00f))
                .body("amountForRecipient", is(90.00f))
                .body("status", equalTo("COMPLETED"))
                .body("createdAt", notNullValue())
                .body("failureReason", nullValue())
                .body("walletTransactionIdRef", notNullValue())
                .body("paymentProviderIdRef", notNullValue())
                .body("recipientFirstName", equalTo("John"))
                .body("recipientLastName", equalTo("Doe"))
                .body("recipientNationalId", equalTo("12345678901"))
                .body("recipientAccountNumber", equalTo("987654321"))
                .body("recipientRoutingNumber", equalTo("123456789"));
    }

    @Test
    void getNonExistingWalletWithdrawalReturns404() {
        String nonExistingId = "123e4567-e89b-12d3-a456-426614174000";

        given()
                .when()
                .get("/api/v1/wallet_withdraw/{id}", nonExistingId)
                .then()
                .statusCode(404);
    }
}