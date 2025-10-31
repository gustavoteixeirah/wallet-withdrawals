package com.teixeirah.withdrawals.infrastructure.secondary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentRequest;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentProviderException;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentRejectedException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
class PaymentProviderAdapter implements PaymentProviderPort {

    private final RestTemplate restTemplate;
    private final String paymentsUrl;

    private record PaymentInfo(String id, BigDecimal amount) {
    }

    private record RequestInfo(String status, String error) {
    }

    private record PaymentResponse(RequestInfo requestInfo, PaymentInfo paymentInfo) {
    }

    public PaymentProviderAdapter(
            @Qualifier("paymentProviderRestTemplate") RestTemplate restTemplate,
            @Value("${adapters.payment-provider.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.paymentsUrl = baseUrl;
    }

    @Override
    @WithSpan(value = "Creating payment")
    public String createPayment(PaymentRequest paymentRequest)
            throws PaymentRejectedException, PaymentProviderException {

        log.atInfo()
                .addKeyValue("amount", paymentRequest.amount())
                .log("payment_create_started");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PaymentRequest> requestEntity = new HttpEntity<>(paymentRequest, headers);

        try {
            PaymentResponse response = restTemplate.postForObject(
                    paymentsUrl,
                    requestEntity,
                    PaymentResponse.class);

            if (response == null || response.paymentInfo() == null || response.paymentInfo().id() == null) {
                log.atError()
                        .addKeyValue("amount", paymentRequest.amount())
                        .log("payment_provider_invalid_response");
                throw new PaymentProviderException("Payment provider returned an invalid response.");
            }

            if ("Failed".equalsIgnoreCase(response.requestInfo().status())) {
                log.atWarn()
                        .addKeyValue("status", response.requestInfo().status())
                        .addKeyValue("error", response.requestInfo().error())
                        .log("payment_rejected_by_provider");
                throw new PaymentRejectedException(response.requestInfo().error() != null ? response.requestInfo().error() : "Payment failed after processing");
            }

            log.atInfo()
                    .addKeyValue("paymentId", response.paymentInfo().id())
                    .log("payment_create_succeeded");
            return response.paymentInfo().id();

        } catch (HttpClientErrorException e) {
            log.atError()
                    .addKeyValue("statusCode", e.getStatusCode())
                    .addKeyValue("responseBody", e.getResponseBodyAsString())
                    .setCause(e)
                    .log("payment_provider_4xx_error");
            throw new PaymentRejectedException("Payment rejected by provider: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.atError()
                    .addKeyValue("statusCode", e.getStatusCode())
                    .addKeyValue("responseBody", e.getResponseBodyAsString())
                    .setCause(e)
                    .log("payment_provider_5xx_error");
            String responseBody = e.getResponseBodyAsString();

            try {
                final var objectMapper = new ObjectMapper();
                PaymentResponse errorResponse = objectMapper.readValue(responseBody, PaymentResponse.class);

                if (errorResponse != null && errorResponse.requestInfo() != null && "Failed".equalsIgnoreCase(errorResponse.requestInfo().status())) {
                    String errorMessage = errorResponse.requestInfo().error() != null ? errorResponse.requestInfo().error() : "Payment failed";
                    log.atWarn()
                            .addKeyValue("status", errorResponse.requestInfo().status())
                            .addKeyValue("error", errorMessage)
                            .log("payment_rejected_on_5xx_body");
                    throw new PaymentRejectedException(errorMessage);
                }

                String errorMessage = (errorResponse != null && errorResponse.requestInfo() != null && errorResponse.requestInfo().error() != null)
                        ? errorResponse.requestInfo().error()
                        : "Unknown server error details";
                log.atError()
                        .addKeyValue("status", errorResponse != null && errorResponse.requestInfo() != null ? errorResponse.requestInfo().status() : null)
                        .addKeyValue("error", errorMessage)
                        .log("payment_provider_5xx_non_failed_or_missing_details");
                throw new PaymentProviderException("Server error from payment provider: " + errorMessage);

            } catch (com.fasterxml.jackson.core.JsonProcessingException parseEx) {
                log.atWarn()
                        .addKeyValue("parseError", parseEx.getMessage())
                        .setCause(parseEx)
                        .log("payment_provider_5xx_error_body_parse_failed");
                throw new PaymentProviderException("Server error from payment provider: " + e.getMessage());
            }
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("message", e.getMessage())
                    .setCause(e)
                    .log("payment_create_unexpected_error");
            throw new PaymentProviderException("Unexpected error communicating with payment provider: " + e.getMessage());
        }
    }
}