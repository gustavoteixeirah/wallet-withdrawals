package com.teixeirah.withdrawals.domain.wallet.withdraw.state;

import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentRequest;
import com.teixeirah.withdrawals.domain.payments.PaymentSource;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentAccount;
import com.teixeirah.withdrawals.domain.payments.PaymentDestination;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentProviderException;
import com.teixeirah.withdrawals.domain.payments.exceptions.PaymentRejectedException;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdraw;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawStatus;
import com.teixeirah.withdrawals.domain.wallet.withdraw.events.WithdrawalCompletedEvent;

public final class WalletDebitedState implements WalletWithdrawState {

    @Override
    public void processPayment(WalletWithdraw context, PaymentProviderPort paymentProviderPort, PaymentSourceProviderPort paymentSourceProviderPort) {
        try {
            PaymentRequest paymentRequest = createPaymentRequest(context, paymentSourceProviderPort);
            String receiptId = paymentProviderPort.createPayment(paymentRequest);
            context.setPaymentProviderIdRef(receiptId);
            context.changeState(new CompletedState(), WalletWithdrawStatus.COMPLETED);
            context.registerDomainEvent(new WithdrawalCompletedEvent(context));
        } catch (PaymentRejectedException e) {
            context.markForCompensation("Payment rejected: " + e.getMessage());
        } catch (PaymentProviderException e) {
            context.markForCompensation("Payment provider error: " + e.getMessage());
        }
    }

    private PaymentRequest createPaymentRequest(WalletWithdraw context, PaymentSourceProviderPort paymentSourceProviderPort) {
        PaymentSource source = paymentSourceProviderPort.getPaymentSource();

        String recipientName = context.getRecipient().firstName() + " " + context.getRecipient().lastName();
        PaymentDestination destination = new PaymentDestination(
                recipientName,
                new PaymentAccount(
                        context.getRecipient().account().accountNumber(),
                        "USD",
                        context.getRecipient().account().routingNumber()
                )
        );

        return new PaymentRequest(source, destination, context.getAmountForRecipient());
    }
}
