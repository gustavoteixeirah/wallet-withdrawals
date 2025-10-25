package com.teixeirah.withdrawals.infrastructure;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.integrations.testcontainers.WireMockContainer;

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

    @Container
    static WireMockContainer wiremock = new WireMockContainer("wiremock/wiremock:latest")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200));

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void registerWireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("adapters.wallet-service.base-url", () -> wiremock.getBaseUrl() + "/wallets/transactions");
        registry.add("adapters.payment-provider.base-url", () -> wiremock.getBaseUrl() + "/api/v1/payments");
    }

    @BeforeEach
    void setupClients() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        WireMock.configureFor(wiremock.getHost(), wiremock.getPort());
        WireMock.reset();
    }

    @Test
    void shouldCreateAndProcessWalletWithdrawalSagaSuccessfully() {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/wallets/transactions"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                            {
                                "wallet_transaction_id": 98765,
                                "amount": -100.00,
                                "user_id": 1
                            }
                        """)));

        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/payments"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                            {
                                "requestInfo": {"status": "Processing"},
                                "paymentInfo": {"amount": 90.00, "id": "payment-abc-123"}
                            }
                        """)));

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

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                given()
                        .when()
                        .get("/api/v1/wallet_withdraw/{id}", transactionId)
                        .then()
                        .statusCode(200)
                        .body("status", anyOf(equalTo("WALLET_DEBITED"), equalTo("COMPLETED")))
        );

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                given()
                        .when()
                        .get("/api/v1/wallet_withdraw/{id}", transactionId)
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("COMPLETED"))
        );

        given()
                .when()
                .get("/api/v1/wallet_withdraw/{id}", transactionId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(transactionId))
                .body("userId", equalTo(1))
                .body("amount", is(100.00f))
                .body("fee", is(10.00f))
                .body("amountForRecipient", is(90.00f))
                .body("status", equalTo("COMPLETED"))
                .body("createdAt", notNullValue())
                .body("failureReason", nullValue())
                .body("walletTransactionIdRef", equalTo("98765"))
                .body("paymentProviderIdRef", equalTo("payment-abc-123"))
                .body("recipientFirstName", equalTo("John"))
                .body("recipientLastName", equalTo("Doe"))
                .body("recipientNationalId", equalTo("12345678901"))
                .body("recipientAccountNumber", equalTo("987654321"))
                .body("recipientRoutingNumber", equalTo("123456789"));
    }

}