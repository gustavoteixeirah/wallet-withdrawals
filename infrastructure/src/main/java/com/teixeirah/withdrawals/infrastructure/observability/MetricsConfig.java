package com.teixeirah.withdrawals.infrastructure.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public LongCounter successWithdrawalStatusCounter(OpenTelemetry openTelemetry) {
        Meter meter = openTelemetry.getMeter("com.teixeirah.withdrawals");
        return meter.counterBuilder("wallet.withdrawal.success.total")
                .setDescription("Counts the number of wallet withdrawals that reach a success state.")
                .build();
    }

    @Bean
    public LongCounter failureWithdrawalStatusCounter(OpenTelemetry openTelemetry) {
        Meter meter = openTelemetry.getMeter("com.teixeirah.withdrawals");
        return meter.counterBuilder("wallet.withdrawal.failure.total")
                .setDescription("Counts the number of wallet withdrawals that reach a failure state.")
                .build();
    }
}