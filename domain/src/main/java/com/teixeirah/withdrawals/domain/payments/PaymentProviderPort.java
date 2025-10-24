package com.teixeirah.withdrawals.domain.payments;

import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentProviderException;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentRejectedException;

public interface PaymentProviderPort {

    String createPayment(PaymentRequest paymentRequest)
            throws PaymentRejectedException, PaymentProviderException;

}
