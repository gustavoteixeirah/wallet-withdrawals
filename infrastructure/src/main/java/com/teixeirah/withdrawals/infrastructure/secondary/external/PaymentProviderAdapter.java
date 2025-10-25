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
import org.springframework.web.client.RestTemplate; // Import RestTemplate

import java.math.BigDecimal;

@Slf4j
@Component
public class PaymentProviderAdapter implements PaymentProviderPort {

    private final RestTemplate restTemplate;
    private final String paymentsUrl;

    // --- DTOs ---
    // PaymentRequest from domain matches API.
    private record PaymentInfo(String id, BigDecimal amount) {}
    private record RequestInfo(String status, String error) {}
    private record PaymentResponse(RequestInfo requestInfo, PaymentInfo paymentInfo) {}

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

            // Handle cases where 200 OK might still mean failure based on mock behavior
            if ("Failed".equalsIgnoreCase(response.requestInfo().status())) {
                log.warn("Payment processed but final status is Failed: {}", response.requestInfo().error());
                throw new PaymentRejectedException(response.requestInfo().error() != null ? response.requestInfo().error() : "Payment failed after processing");
            }

            log.info("RestTemplate createPayment call successful. Response ID: {}", response.paymentInfo().id());
            return response.paymentInfo().id();

        } catch (HttpClientErrorException e) {
            // Treat most 4xx as rejections
            log.error("Payment Provider 4xx Error: Status {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new PaymentRejectedException("Payment rejected by provider: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("Payment Provider 5xx Error: Status {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            // Attempt to parse body for specific rejection message based on mock behavior
            try {
                PaymentResponse errorResponse = e.getResponseBodyAs(PaymentResponse.class);
                if (errorResponse != null && "Failed".equalsIgnoreCase(errorResponse.requestInfo().status())) {
                    throw new PaymentRejectedException(errorResponse.requestInfo().error() != null ? errorResponse.requestInfo().error() : "Payment failed");
                }
            } catch (Exception parseEx) {
                log.warn("Could not parse 5xx error body for rejection details: {}", parseEx.getMessage());
            }
            // If not parsed as rejection, treat as provider error
            throw new PaymentProviderException("Server error from payment provider: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during RestTemplate createPayment call: {}", e.getMessage(), e);
            throw new PaymentProviderException("Unexpected error communicating with payment provider: " + e.getMessage());
        }
    }
}