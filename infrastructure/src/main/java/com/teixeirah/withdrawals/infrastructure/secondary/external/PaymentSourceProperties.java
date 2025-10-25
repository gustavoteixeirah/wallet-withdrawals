package com.teixeirah.withdrawals.infrastructure.secondary.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapters.payment-source")
public record PaymentSourceProperties(
        String type,
        SourceInformationProperties sourceInformation,
        AccountProperties account
) {
    public record SourceInformationProperties(String name) {
    }

    public record AccountProperties(
            String accountNumber,
            String currency,
            String routingNumber
    ) {
    }
}