package com.teixeirah.withdrawals.infrastructure.secondary.external.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpAdaptersConfiguration {

    @Bean("walletServiceRestTemplate")
    public RestTemplate walletServiceRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean("paymentProviderRestTemplate")
    public RestTemplate paymentProviderRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean("walletBalanceRestTemplate")
    public RestTemplate walletBalanceRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}