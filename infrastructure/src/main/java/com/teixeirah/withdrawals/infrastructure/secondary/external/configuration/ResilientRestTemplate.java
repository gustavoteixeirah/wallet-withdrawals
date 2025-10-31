package com.teixeirah.withdrawals.infrastructure.secondary.external.configuration;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.function.Supplier;

class ResilientRestTemplate extends RestTemplate {

    private static final Logger log = LoggerFactory.getLogger(ResilientRestTemplate.class);

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;
    private final String instanceName;

    public ResilientRestTemplate(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            BulkheadRegistry bulkheadRegistry,
            String instanceName
    ) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(instanceName);
        this.retry = retryRegistry.retry(instanceName);
        this.bulkhead = bulkheadRegistry.bulkhead(instanceName);
        this.instanceName = instanceName;
    }

    @Override
    protected <T> T doExecute(
            URI url,
            @Nullable HttpMethod method,
            @Nullable RequestCallback requestCallback,
            @Nullable ResponseExtractor<T> responseExtractor
    ) throws RestClientException {
        Supplier<T> supplier = () -> super.doExecute(url, method, requestCallback, responseExtractor);

        supplier = Bulkhead.decorateSupplier(bulkhead, supplier);
        supplier = Retry.decorateSupplier(retry, supplier);
        supplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);

        try {
            return supplier.get();
        } catch (CallNotPermittedException e) {
            log.error("Circuit open for service: {}", instanceName, e);
            throw new RestClientException("Service unavailable due to circuit breaker: " + instanceName, e); // Will be caught by adapter's try-catch
        } catch (Exception e) {
            log.error("Resilient execution failed for service: {}", instanceName, e);
            if (e.getCause() instanceof RestClientException) {
                throw (RestClientException) e.getCause(); // Unwrap to preserve original exceptions for adapter handling
            }
            throw new RestClientException("Resilient call failed: " + e.getMessage(), e);
        }
    }
}