package com.teixeirah.withdrawals.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "adapters.payment-provider")
public class PaymentProviderProperties {

    private String baseUrl = "http://localhost:8082";
    private String createPaymentEndpoint = "/api/payments";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getCreatePaymentEndpoint() {
        return createPaymentEndpoint;
    }

    public void setCreatePaymentEndpoint(String createPaymentEndpoint) {
        this.createPaymentEndpoint = createPaymentEndpoint;
    }
}