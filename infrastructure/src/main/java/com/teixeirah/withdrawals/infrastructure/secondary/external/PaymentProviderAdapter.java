package com.teixeirah.withdrawals.infrastructure.secondary.external;

import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentRequest;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentProviderException;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentRejectedException;
import lombok.extern.slf4j.Slf4j;
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
            RestTemplate restTemplate, // Inject RestTemplate
            @Value("${adapters.payment-provider.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.paymentsUrl = baseUrl + "/api/v1/payments"; // Construct full URL
    }

    @Override
    public String createPayment(PaymentRequest paymentRequest)
            throws PaymentRejectedException, PaymentProviderException {

        log.info("Creating payment via RestTemplate: amount={}", paymentRequest.amount());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PaymentRequest> requestEntity = new HttpEntity<>(paymentRequest, headers);

        try {
            PaymentResponse response = restTemplate.postForObject(
                    paymentsUrl,
                    requestEntity,
                    PaymentResponse.class);

            if (response == null || response.paymentInfo() == null || response.paymentInfo().id() == null) {
                log.error("Received null or incomplete response from payment provider for amount={}", paymentRequest.amount());
                throw new PaymentProviderException("Payment provider returned an invalid response.");
            }

            if ("Failed".equalsIgnoreCase(response.requestInfo().status())) {
                log.warn("Payment processed but final status is Failed: {}", response.requestInfo().error());
                throw new PaymentRejectedException(response.requestInfo().error() != null ? response.requestInfo().error() : "Payment failed after processing");
            }

            log.info("RestTemplate createPayment call successful. Response ID: {}", response.paymentInfo().id());
            return response.paymentInfo().id();

        } catch (HttpClientErrorException e) {
            log.error("Payment Provider 4xx Error: Status {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new PaymentRejectedException("Payment rejected by provider: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Payment Provider 5xx Error: Status {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            String responseBody = e.getResponseBodyAsString();

            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                PaymentResponse errorResponse = objectMapper.readValue(responseBody, PaymentResponse.class);

                if (errorResponse != null && errorResponse.requestInfo() != null && "Failed".equalsIgnoreCase(errorResponse.requestInfo().status())) {
                    String errorMessage = errorResponse.requestInfo().error() != null ? errorResponse.requestInfo().error() : "Payment failed";
                    log.warn("Payment provider returned 5xx but indicates business failure: {}", errorMessage);
                    throw new PaymentRejectedException(errorMessage);
                }

                String errorMessage = (errorResponse != null && errorResponse.requestInfo() != null && errorResponse.requestInfo().error() != null)
                        ? errorResponse.requestInfo().error()
                        : "Unknown server error details";
                log.error("Payment provider returned 5xx with non-Failed status or missing details: {}", errorMessage);
                throw new PaymentProviderException("Server error from payment provider: " + errorMessage);

            } catch (com.fasterxml.jackson.core.JsonProcessingException parseEx) {
                log.warn("Could not parse 5xx error body as PaymentResponse JSON: {}", parseEx.getMessage());
                throw new PaymentProviderException("Server error from payment provider: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error during RestTemplate createPayment call: {}", e.getMessage(), e);
            throw new PaymentProviderException("Unexpected error communicating with payment provider: " + e.getMessage());
        }
    }
}