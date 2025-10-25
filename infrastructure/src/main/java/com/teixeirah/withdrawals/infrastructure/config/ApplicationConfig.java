package com.teixeirah.withdrawals.infrastructure.config;

import com.teixeirah.withdrawals.application.input.InitiateWalletWithdrawInputPort;
import com.teixeirah.withdrawals.application.input.ProcessPaymentInputPort;
import com.teixeirah.withdrawals.application.input.ProcessWalletDebitInputPort;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public InitiateWalletWithdrawInputPort initiateWalletWithdrawInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            DomainEventPublisherPort eventPublisher) {
        return new InitiateWalletWithdrawInputPort(walletWithdrawRepository, eventPublisher);
    }

    @Bean
    public ProcessWalletDebitInputPort processWalletDebitInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            WalletServicePort walletServicePort,
            DomainEventPublisherPort eventPublisher) {
        return new ProcessWalletDebitInputPort(walletWithdrawRepository, walletServicePort, eventPublisher);
    }

    @Bean
    public ProcessPaymentInputPort processPaymentInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            PaymentProviderPort paymentProviderPort,
            PaymentSourceProviderPort paymentSourceProviderPort,
            DomainEventPublisherPort eventPublisher) {
        return new ProcessPaymentInputPort(walletWithdrawRepository, paymentProviderPort, paymentSourceProviderPort, eventPublisher);
    }
}