package com.teixeirah.withdrawals.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;

@TestConfiguration
public class TestEventPublishingFailureConfig {

    @Bean("tracingDomainEventPublisherDecorator")
    @Primary
    public DomainEventPublisherPort failingDomainEventPublisher() {
        return events -> {
            throw new RuntimeException("Simulated event publishing failure");
        };
    }
}