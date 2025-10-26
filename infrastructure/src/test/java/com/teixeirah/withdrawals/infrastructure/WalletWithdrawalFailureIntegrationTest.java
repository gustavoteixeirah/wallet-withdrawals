package com.teixeirah.withdrawals.infrastructure;

import java.time.Duration;

import io.restassured.http.ContentType;
import org.hamcrest.Matcher;
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
import static org.hamcrest.Matchers.startsWith;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletWithdrawalFailureIntegrationTest {

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
    void shouldFailWithReasonWhenWalletDebitReturns404() {
        stubWalletBalanceHigh();
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/wallets/transactions"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(404)
                        .withBody("""
                                    { "code": "INVALID_USER", "message": "user not found" }
                                """)));

        String transactionId = initiateWithdrawal();

        awaitStatusAndVerifyReason(transactionId, "Wallet not found");
    }

    @Test
    void shouldFailWithReasonWhenWalletDebitReturns409() {
        stubWalletBalanceHigh();
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/wallets/transactions"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(409)
                        .withBody("""
                                    { "code": "INSUFFICIENT_FUNDS", "message": "User balance is too low" }
                                """)));

        String transactionId = initiateWithdrawal();

        awaitStatusAndVerifyReason(transactionId, "Insufficient funds");
    }

    @Test
    void shouldFailWithReasonWhenWalletDebitReturns500() {
        stubWalletBalanceHigh();
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/wallets/transactions"))
        .willReturn(WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500)
            .withBody("""
                    { "code": "GENERIC_ERROR", "message": "something bad happened" }
                """)));

        String transactionId = initiateWithdrawal();

        awaitStatusAndVerifyReason(transactionId, startsWith("Wallet service error:"));
    }

    @Test
    void shouldFailWithReasonWhenPaymentProviderReturns400() {
        stubWalletBalanceHigh();
        stubWalletServiceSuccess();
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/payments"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
            .withStatus(400)
                        .withBody("""
                                    { "error": "body is invalid, check postman collection example" }
                                """)));

        String transactionId = initiateWithdrawal();

        awaitStatusAndVerifyReason(transactionId, startsWith("Payment rejected:"));
    }

    @Test
    void shouldFailWithReasonWhenPaymentProviderReturns500WithErrorStatusInBody() {
        stubWalletBalanceHigh();
        stubWalletServiceSuccess();
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/payments"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
            .withStatus(500)
                        .withBody("""
                                    {
                                        "requestInfo": {"status": "Failed", "error": "bank rejected payment"},
                                        "paymentInfo": {"amount": 90.00, "id": "payment-failed-123"}
                                    }
                                """)));

        String transactionId = initiateWithdrawal();

        awaitStatusAndVerifyReason(transactionId, "Payment rejected: bank rejected payment");
    }

    @Test
    void shouldFailWithReasonWhenPaymentProviderReturns500GenericError() {
        stubWalletBalanceHigh();
        stubWalletServiceSuccess();
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/payments"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
            .withStatus(500)
                        .withBody("""
                                    {
                                        "requestInfo": {"status": "Error", "error": "Internal Server Error"},
                                        "paymentInfo": null
                                    }
                                """)));

        String transactionId = initiateWithdrawal();

        awaitStatusAndVerifyReason(transactionId, startsWith("Payment provider error:"));
    }

    @Test
    void shouldFailEarlyWhenBalanceIsInsufficient() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/wallets/balance"))
                .withQueryParam("user_id", WireMock.equalTo("1"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                    { "balance": 50.00, "user_id": 1 }
                                """)));

        stubWalletServiceSuccess();

        String transactionId = initiateWithdrawal();

        awaitStatusAndVerifyReason(transactionId, "Insufficient funds");
        WireMock.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/wallets/transactions")));
    }

    private String initiateWithdrawal() {
        return given()
                .contentType(ContentType.JSON)
                .body(walletWithdrawalRequest()
                        .withRecipient("Test", "Fail")
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

    private void awaitStatusAndVerifyReason(String transactionId, String expectedReason) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                given()
                        .when()
                        .get("/api/v1/wallet_withdraw/{id}", transactionId)
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("FAILED"))
        );

        given()
                .when()
                .get("/api/v1/wallet_withdraw/{id}", transactionId)
                .then()
                .statusCode(200)
                .body("status", equalTo("FAILED"))
                .body("failureReason", equalTo(expectedReason));
    }

    private void awaitStatusAndVerifyReason(String transactionId, Matcher<String> reasonMatcher) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                given()
                        .when()
                        .get("/api/v1/wallet_withdraw/{id}", transactionId)
                        .then()
                        .statusCode(200)
                        .body("status", equalTo("FAILED"))
        );

        given()
                .when()
                .get("/api/v1/wallet_withdraw/{id}", transactionId)
                .then()
                .statusCode(200)
                .body("status", equalTo("FAILED"))
                .body("failureReason", reasonMatcher);
    }

    private void stubWalletServiceSuccess() {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/wallets/transactions"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                    { "wallet_transaction_id": 12345, "amount": -110.00, "user_id": 1 }
                                """)));
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
}