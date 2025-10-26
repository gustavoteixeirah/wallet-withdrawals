package com.teixeirah.withdrawals.infrastructure.support;

import com.teixeirah.withdrawals.domain.payments.PaymentAccount;
import com.teixeirah.withdrawals.domain.payments.PaymentSource;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceInformation;

public final class PaymentSourceTestDataBuilder {

    private String type = "COMPANY";
    private String companyName = "ONTOP INC";
    private String accountNumber = "0245253419";
    private String currency = "USD";
    private String routingNumber = "028444018";

    public static PaymentSourceTestDataBuilder paymentSource() {
        return new PaymentSourceTestDataBuilder();
    }

    public PaymentSourceTestDataBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public PaymentSourceTestDataBuilder withCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public PaymentSourceTestDataBuilder withAccountDetails(String accountNumber, String currency, String routingNumber) {
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.routingNumber = routingNumber;
        return this;
    }

    public PaymentSource build() {
        return new PaymentSource(
                type,
                new PaymentSourceInformation(companyName),
                new PaymentAccount(accountNumber, currency, routingNumber)
        );
    }
}
