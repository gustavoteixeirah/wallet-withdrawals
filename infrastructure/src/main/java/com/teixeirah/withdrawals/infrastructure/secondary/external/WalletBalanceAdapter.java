package com.teixeirah.withdrawals.infrastructure.secondary.external;

import com.teixeirah.withdrawals.domain.wallet.service.WalletBalancePort;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletNotFoundException;
import com.teixeirah.withdrawals.domain.wallet.service.exceptions.WalletServiceException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

@Component
public class WalletBalanceAdapter implements WalletBalancePort {

    private static final Logger log = LoggerFactory.getLogger(WalletBalanceAdapter.class);

    private final RestTemplate restTemplate;
    private final String balanceBaseUrl;

    private record BalanceResponse(BigDecimal balance, long user_id) {
    }

    public WalletBalanceAdapter(
            @Qualifier("walletBalanceRestTemplate") RestTemplate restTemplate,
            @Value("${adapters.wallet-balance.base-url:http://mockoon.tools.getontop.com:3000/wallets/balance}") String balanceBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.balanceBaseUrl = balanceBaseUrl;
    }

    @Override
    @WithSpan(value = "Getting wallet balance")
    public BigDecimal getBalance(Long userId) throws WalletNotFoundException, WalletServiceException {
        var uri = UriComponentsBuilder.fromHttpUrl(balanceBaseUrl)
                .queryParam("user_id", userId)
                .build(true)
                .toUri();

        try {
            log.atInfo().addKeyValue("userId", userId).log("wallet_balance_check_attempt");
            ResponseEntity<BalanceResponse> response = restTemplate.getForEntity(uri, BalanceResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.atError().addKeyValue("statusCode", response.getStatusCode()).log("wallet_balance_invalid_response");
                throw new WalletServiceException("Invalid response from wallet balance service");
            }

            log.atInfo().addKeyValue("balance", response.getBody().balance()).log("wallet_balance_check_success");
            return response.getBody().balance();
        } catch (HttpClientErrorException e) {
            log.atError()
                    .addKeyValue("statusCode", e.getStatusCode())
                    .addKeyValue("responseBody", e.getResponseBodyAsString())
                    .setCause(e)
                    .log("wallet_balance_4xx_error");
            if (e.getStatusCode().value() == 404) {
                throw new WalletNotFoundException("Wallet not found: " + e.getResponseBodyAsString());
            }
            throw new WalletServiceException("Client error calling wallet balance service: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            log.atError()
                    .addKeyValue("statusCode", e.getStatusCode())
                    .addKeyValue("responseBody", e.getResponseBodyAsString())
                    .setCause(e)
                    .log("wallet_balance_5xx_error");
            throw new WalletServiceException("Server error from wallet balance service: " + e.getMessage());
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("errorMessage", e.getMessage())
                    .setCause(e)
                    .log("wallet_balance_unexpected_error");
            throw new WalletServiceException("Unexpected error communicating with wallet balance service: " + e.getMessage());
        }
    }
}
