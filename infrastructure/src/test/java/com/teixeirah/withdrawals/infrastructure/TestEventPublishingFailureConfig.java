package com.teixeirah.withdrawals.infrastructure;

import com.teixeirah.withdrawals.domain.events.DomainEvent;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@TestConfiguration
public class TestEventPublishingFailureConfig {

    @Bean
    @Primary
    public DomainEventPublisherPort failingDomainEventPublisher() {
        return events -> {
            throw new RuntimeException("Simulated event publishing failure");
        };
    }
}