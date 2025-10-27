package com.teixeirah.withdrawals.infrastructure;

import java.math.BigDecimal;
import java.time.Duration;

import io.restassured.http.ContentType;
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

import com.github.tomakehurst.wiremock.client.WireMock;

import static com.teixeirah.withdrawals.infrastructure.support.RestAssuredTestSupport.configureForPort;
import static com.teixeirah.withdrawals.infrastructure.support.WalletWithdrawalRequestBuilder.walletWithdrawalRequest;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletCompensationIntegrationTest {

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
        registry.add("adapters.wallet-balance.base-url", () -> wiremock.getBaseUrl() + "/wallets/balance");
        registry.add("adapters.payment-provider.base-url", () -> wiremock.getBaseUrl() + "/api/v1/payments");
    }

    @BeforeEach
    void setupClients() {
        configureForPort(port);
        WireMock.configureFor(wiremock.getHost(), wiremock.getPort());
        WireMock.reset();
    }

    @Test
    void shouldExecuteWalletRefundWhenPaymentFails() {
        stubWalletBalanceHigh();
        stubWalletServiceDebitSuccess();
        stubPaymentProviderFailure();
        stubWalletServiceRefundSuccess();

        String transactionId = initiateWithdrawal();

        awaitCompensationCompletion(transactionId);

        verifyWalletRefundCalled(transactionId);
    }

    @Test
    void shouldMarkWithdrawalAsFailedWhenRefundFails() {
        stubWalletBalanceHigh();
        stubWalletServiceDebitSuccess();
        stubPaymentProviderFailure();
        stubWalletServiceRefundFailure();

        String transactionId = initiateWithdrawal();

        awaitCompensationFailure(transactionId);

        verifyWalletRefundCalled(transactionId);
    }

    @Test
    void shouldReturnBadRequestWhenValidationFails() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "userId": 1,
                            "amount": 100.00,
                            "recipientFirstName": "Test",
                            "recipientLastName": "User",
                            "recipientNationalId": "123",
                            "recipientAccountNumber": "123456",
                            "recipientRoutingNumber": ""
                        }
                        """)
                .when()
                .post("/api/v1/wallet_withdraw")
                .then()
                .statusCode(400)
                .body("message", equalTo("Account routing number is required."));
    }

    @Test
    void shouldReturnBadRequestWhenUserIdIsMissing() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "amount": 100.00,
                            "recipientFirstName": "Test",
                            "recipientLastName": "User",
                            "recipientNationalId": "123",
                            "recipientAccountNumber": "123456",
                            "recipientRoutingNumber": "789"
                        }
                        """)
                .when()
                .post("/api/v1/wallet_withdraw")
                .then()
                .statusCode(400)
                .body("message", equalTo("User ID cannot be null"));
    }

    private String initiateWithdrawal() {
        return given()
                .contentType(ContentType.JSON)
                .body(walletWithdrawalRequest()
                        .withRecipient("Test", "Refund")
                        .withRecipientAccount("333", "111")
                        .withRecipientNationalId("222")
                        .build())
                .when()
                .post("/api/v1/wallet_withdraw")
                .then()
                .statusCode(200)
                .body("status", equalTo("CREATED"))
                .extract().jsonPath().getString("transactionId");
    }

    private void awaitCompensationCompletion(String transactionId) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                given()
                        .when()
                        .get("/api/v1/wallet_withdraw/{id}", transactionId)
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("REFUNDED"))
        );
    }

    private void awaitCompensationFailure(String transactionId) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                given()
                        .when()
                        .get("/api/v1/wallet_withdraw/{id}", transactionId)
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("FAILED"))
                        .body("failureReason", matchesPattern("Compensation failed: Server error from wallet service: 500 Server Error on POST request for \"http://localhost:\\d+/wallets/transactions\": \\[no body\\]"))
        );
    }

    private void verifyWalletRefundCalled(String transactionId) {
        WireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/wallets/transactions"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "amount": 110.00,
                            "user_id": 1
                        }
                        """))
        );
    }

    private void stubWalletBalanceHigh() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/wallets/balance"))
                .withQueryParam("user_id", WireMock.equalTo("1"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                { "balance": 100000.00, "user_id": 1 }
                                """)));
    }

    private void stubWalletServiceDebitSuccess() {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/wallets/transactions"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "amount": -110.00,
                            "user_id": 1
                        }
                        """))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                { "wallet_transaction_id": 12345, "amount": -110.00, "user_id": 1 }
                                """)));
    }

    private void stubPaymentProviderFailure() {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/payments"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(400)
                        .withBody("""
                                { "error": "payment rejected by bank" }
                                """)));
    }

    private void stubWalletServiceRefundSuccess() {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/wallets/transactions"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "amount": 110.00,
                            "user_id": 1
                        }
                        """))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                { "wallet_transaction_id": 54321, "amount": 110.00, "user_id": 1 }
                                """)));
    }

    private void stubWalletServiceRefundFailure() {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/wallets/transactions"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                            "amount": 110.00,
                            "user_id": 1
                        }
                        """))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(500)
                        .withBody("")));
    }
}