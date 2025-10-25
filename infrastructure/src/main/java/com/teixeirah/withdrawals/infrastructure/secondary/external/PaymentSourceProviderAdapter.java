package com.teixeirah.withdrawals.infrastructure.secondary.external;

import com.teixeirah.withdrawals.domain.payments.PaymentAccount;
import com.teixeirah.withdrawals.domain.payments.PaymentSource;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceInformation;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import com.teixeirah.withdrawals.infrastructure.config.PaymentSourceProperties;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentSourceProviderAdapter implements PaymentSourceProviderPort {

    private final PaymentSourceProperties paymentSourceProperties;

    public PaymentSourceProviderAdapter(PaymentSourceProperties paymentSourceProperties) {
        this.paymentSourceProperties = paymentSourceProperties;
    }

    @Override
    @WithSpan(value = "get_payment_source")
    public PaymentSource getPaymentSource() {
        log.atInfo()
           .addKeyValue("operation", "getPaymentSource")
           .log("payment_source_retrieval_started");

        PaymentSource source = new PaymentSource(
                paymentSourceProperties.getType(),
                new PaymentSourceInformation(paymentSourceProperties.getName()),
                new PaymentAccount(
                        paymentSourceProperties.getAccountNumber(),
                        paymentSourceProperties.getCurrency(),
                        paymentSourceProperties.getRoutingNumber()
                )
        );

        log.atInfo()
           .addKeyValue("type", paymentSourceProperties.getType())
           .addKeyValue("name", paymentSourceProperties.getName())
           .log("payment_source_retrieved");
        return source;
    }
}