package com.teixeirah.withdrawals.infrastructure.secondary.external;

import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.InsufficientFundsException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletServiceException;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class WalletServiceAdapter implements WalletServicePort {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceAdapter.class);

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;

    private record WalletTransactionRequest(BigDecimal amount, long user_id) {
    }

    private record WalletTransactionResponse(long wallet_transaction_id, BigDecimal amount, long user_id) {
    }

    public WalletServiceAdapter(
            @Qualifier("walletServiceRestTemplate") RestTemplate restTemplate,
            @Value("${adapters.wallet-service.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = baseUrl;
    }

    @Override
    @WithSpan(value = "Debiting wallet")
    public Long debit(@SpanAttribute("wallet.user.id") Long userId,
                      @SpanAttribute("wallet.amount") BigDecimal amount,
                      @SpanAttribute("wallet.transaction.id") UUID transactionId)
            throws InsufficientFundsException, WalletNotFoundException, WalletServiceException {

        log.atInfo()
                .addKeyValue("userId", userId)
                .addKeyValue("amount", amount)
                .addKeyValue("transactionId", transactionId)
                .log("wallet_service_debit_attempt");
        var requestPayload = new WalletTransactionRequest(amount.negate(), userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WalletTransactionRequest> requestEntity = new HttpEntity<>(requestPayload, headers);

        try {
            WalletTransactionResponse response = restTemplate.postForObject(
                    walletServiceUrl,
                    requestEntity,
                    WalletTransactionResponse.class);

            if (response == null) {
                log.atError()
                        .addKeyValue("userId", userId)
                        .log("wallet_service_null_response");
                throw new WalletServiceException("Wallet service returned an empty response.");
            }

            log.atInfo()
                    .addKeyValue("walletTransactionId", response.wallet_transaction_id())
                    .log("wallet_service_debit_success");
            return response.wallet_transaction_id();

        } catch (HttpClientErrorException e) {
            log.atError()
                    .addKeyValue("statusCode", e.getStatusCode())
                    .addKeyValue("responseBody", e.getResponseBodyAsString())
                    .setCause(e)
                    .log("wallet_service_4xx_error");
            if (e.getStatusCode().value() == 404) {
                throw new WalletNotFoundException("Wallet not found: " + e.getResponseBodyAsString());
            }
            if (e.getStatusCode().value() == 409) {
                throw new InsufficientFundsException("Insufficient funds: " + e.getResponseBodyAsString());
            }
            throw new WalletServiceException("Client error calling wallet service: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            log.atError()
                    .addKeyValue("statusCode", e.getStatusCode())
                    .addKeyValue("responseBody", e.getResponseBodyAsString())
                    .setCause(e)
                    .log("wallet_service_5xx_error");
            throw new WalletServiceException("Server error from wallet service: " + e.getMessage());
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("errorMessage", e.getMessage())
                    .setCause(e)
                    .log("wallet_service_debit_unexpected_error");
            throw new WalletServiceException("Unexpected error communicating with wallet service: " + e.getMessage());
        }
    }

    @Override
    @WithSpan(value = "Topping up wallet")
    public Long topUp(@SpanAttribute("wallet.user.id") Long userId,
                      @SpanAttribute("wallet.amount") BigDecimal amount,
                      @SpanAttribute("wallet.transaction.id") UUID transactionId)
            throws WalletNotFoundException, WalletServiceException {

        log.atInfo()
                .addKeyValue("userId", userId)
                .addKeyValue("amount", amount)
                .addKeyValue("transactionId", transactionId)
                .log("wallet_service_topup_attempt");
        var requestPayload = new WalletTransactionRequest(amount, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WalletTransactionRequest> requestEntity = new HttpEntity<>(requestPayload, headers);

        try {
            WalletTransactionResponse response = restTemplate.postForObject(
                    walletServiceUrl,
                    requestEntity,
                    WalletTransactionResponse.class);

            if (response == null) {
                log.atError()
                        .addKeyValue("userId", userId)
                        .log("wallet_service_null_response");
                throw new WalletServiceException("Wallet service returned an empty response.");
            }

            log.atInfo()
                    .addKeyValue("walletTransactionId", response.wallet_transaction_id())
                    .log("wallet_service_topup_success");
            return response.wallet_transaction_id();

        } catch (HttpClientErrorException e) {
            log.atError()
                    .addKeyValue("statusCode", e.getStatusCode())
                    .addKeyValue("responseBody", e.getResponseBodyAsString())
                    .setCause(e)
                    .log("wallet_service_4xx_error");
            if (e.getStatusCode().value() == 404) {
                throw new WalletNotFoundException("Wallet not found: " + e.getResponseBodyAsString());
            }
            throw new WalletServiceException("Client error calling wallet service: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            log.atError()
                    .addKeyValue("statusCode", e.getStatusCode())
                    .addKeyValue("responseBody", e.getResponseBodyAsString())
                    .setCause(e)
                    .log("wallet_service_5xx_error");
            throw new WalletServiceException("Server error from wallet service: " + e.getMessage());
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("errorMessage", e.getMessage())
                    .setCause(e)
                    .log("wallet_service_topup_unexpected_error");
            throw new WalletServiceException("Unexpected error communicating with wallet service: " + e.getMessage());
        }
    }}
