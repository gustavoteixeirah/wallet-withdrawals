package com.teixeirah.withdrawals.infrastructure.secondary.external;

import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.InsufficientFundsException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate; // Import RestTemplate

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class WalletServiceAdapter implements WalletServicePort {

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;

    // --- DTOs ---
    private record WalletTransactionRequest(BigDecimal amount, long user_id) {}
    private record WalletTransactionResponse(long wallet_transaction_id, BigDecimal amount, long user_id) {}
    // We might need an error DTO if the API returns structured errors, but let's keep it simple for now.

    public WalletServiceAdapter(
            RestTemplate restTemplate, // Inject RestTemplate
            @Value("${adapters.wallet-service.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = baseUrl + "/wallets/transactions"; // Construct full URL
    }

    @Override
    public Long debit(Long userId, BigDecimal amount, UUID transactionId)
            throws InsufficientFundsException, WalletNotFoundException, WalletServiceException {

        log.info("Attempting debit via RestTemplate: userId={}, amount={}, transactionId={}", userId, amount, transactionId);
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
                log.error("Received null response from wallet service for debit userId={}", userId);
                throw new WalletServiceException("Wallet service returned an empty response.");
            }

            log.info("RestTemplate debit call successful. Response ID: {}", response.wallet_transaction_id());
            return response.wallet_transaction_id();

        } catch (HttpClientErrorException e) {
            log.error("Wallet Service 4xx Error: Status {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (e.getStatusCode().value() == 404) {
                throw new WalletNotFoundException("Wallet not found: " + e.getResponseBodyAsString());
            }
            if (e.getStatusCode().value() == 409) { // Example for conflict/insufficient funds
                throw new InsufficientFundsException("Insufficient funds: " + e.getResponseBodyAsString());
            }
            throw new WalletServiceException("Client error calling wallet service: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            log.error("Wallet Service 5xx Error: Status {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new WalletServiceException("Server error from wallet service: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during RestTemplate debit call: {}", e.getMessage(), e);
            throw new WalletServiceException("Unexpected error communicating with wallet service: " + e.getMessage());
        }
    }
}