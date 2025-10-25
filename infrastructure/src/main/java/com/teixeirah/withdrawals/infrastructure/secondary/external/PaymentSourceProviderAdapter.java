package com.teixeirah.withdrawals.infrastructure.secondary.external;

import com.teixeirah.withdrawals.domain.payments.PaymentAccount;
import com.teixeirah.withdrawals.domain.payments.PaymentSource;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceInformation;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentSourceProviderAdapter implements PaymentSourceProviderPort {

    @Override
    public PaymentSource getPaymentSource() {
        log.info("Retrieving payment source information");

        PaymentSource source = new PaymentSource(
                "COMPANY",
                new PaymentSourceInformation("ONTOP INC"),
                new PaymentAccount("0245253419", "USD", "028444018")
        );

        log.info("Payment source retrieved: {}", source);
        return source;
    }
}