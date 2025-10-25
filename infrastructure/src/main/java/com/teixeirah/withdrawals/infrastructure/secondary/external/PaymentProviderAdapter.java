package com.teixeirah.withdrawals.infrastructure.secondary.external;

import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentRequest;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentProviderException;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentRejectedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentProviderAdapter implements PaymentProviderPort {

    @Override
    public String createPayment(PaymentRequest paymentRequest) throws PaymentRejectedException, PaymentProviderException {
        log.info("Creating payment: source={}, destination={}, amount={}",
                paymentRequest.source(),
                paymentRequest.destination(),
                paymentRequest.amount());

        // Simulate successful payment creation
        String receiptId = "receipt-" + System.currentTimeMillis();
        log.info("Payment created successfully with receipt: {}", receiptId);

        return receiptId;
    }
}