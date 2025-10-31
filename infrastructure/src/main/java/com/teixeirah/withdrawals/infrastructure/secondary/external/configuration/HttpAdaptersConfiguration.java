package com.teixeirah.withdrawals.infrastructure.secondary.external.configuration;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
class HttpAdaptersConfiguration {

    @Bean("walletServiceRestTemplate")
    public RestTemplate walletServiceRestTemplate(
            RestTemplateBuilder builder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            BulkheadRegistry bulkheadRegistry
    ) {
        ResilientRestTemplate resilientRestTemplate = new ResilientRestTemplate(circuitBreakerRegistry, retryRegistry, bulkheadRegistry, "walletService");
        resilientRestTemplate.setUriTemplateHandler(builder.build().getUriTemplateHandler());
        return resilientRestTemplate;
    }

    @Bean("paymentProviderRestTemplate")
    public RestTemplate paymentProviderRestTemplate(
            RestTemplateBuilder builder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            BulkheadRegistry bulkheadRegistry
    ) {
        ResilientRestTemplate resilientRestTemplate = new ResilientRestTemplate(circuitBreakerRegistry, retryRegistry, bulkheadRegistry, "paymentProvider");
        resilientRestTemplate.setUriTemplateHandler(builder.build().getUriTemplateHandler());
        return resilientRestTemplate;
    }

    @Bean("walletBalanceRestTemplate")
    public RestTemplate walletBalanceRestTemplate(
            RestTemplateBuilder builder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            BulkheadRegistry bulkheadRegistry
    ) {
        ResilientRestTemplate resilientRestTemplate = new ResilientRestTemplate(circuitBreakerRegistry, retryRegistry, bulkheadRegistry, "walletBalance");
        resilientRestTemplate.setUriTemplateHandler(builder.build().getUriTemplateHandler());
        return resilientRestTemplate;
    }
}