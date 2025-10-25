package com.teixeirah.withdrawals.infrastructure.secondary.external;

import com.teixeirah.withdrawals.domain.payments.PaymentAccount;
import com.teixeirah.withdrawals.domain.payments.PaymentSource;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceInformation;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableConfigurationProperties(PaymentSourceProperties.class)
public class PaymentSourceProviderAdapter implements PaymentSourceProviderPort {

    private final PaymentSource paymentSource;

    public PaymentSourceProviderAdapter(PaymentSourceProperties properties) {
        log.info("Initializing PaymentSource from configuration...");

        var account = new PaymentAccount(
                properties.account().accountNumber(),
                properties.account().currency(),
                properties.account().routingNumber()
        );

        var info = new PaymentSourceInformation(
                properties.sourceInformation().name()
        );

        this.paymentSource = new PaymentSource(
                properties.type(),
                info,
                account
        );

        log.info("PaymentSource initialized successfully.");
    }

    @Override
    public PaymentSource getPaymentSource() {
        return this.paymentSource;
    }
}