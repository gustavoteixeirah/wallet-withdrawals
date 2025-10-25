package com.teixeirah.withdrawals.infrastructure.config;

import com.teixeirah.withdrawals.application.input.GetWalletWithdrawInputPort;
import com.teixeirah.withdrawals.application.input.InitiateWalletWithdrawInputPort;
import com.teixeirah.withdrawals.application.input.ProcessPaymentInputPort;
import com.teixeirah.withdrawals.application.input.ProcessWalletDebitInputPort;
import com.teixeirah.withdrawals.application.usecase.GetWalletWithdrawUseCase;
import com.teixeirah.withdrawals.application.usecase.InitiateWalletWithdrawalUseCase;
import com.teixeirah.withdrawals.application.usecase.ProcessPaymentUseCase;
import com.teixeirah.withdrawals.application.usecase.ProcessWalletDebitUseCase;
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
    public InitiateWalletWithdrawalUseCase initiateWalletWithdrawInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            DomainEventPublisherPort eventPublisher) {

        return new InitiateWalletWithdrawInputPort(walletWithdrawRepository, eventPublisher);
    }

    @Bean
    public GetWalletWithdrawUseCase getWalletWithdrawInputPort(
            WalletWithdrawRepository walletWithdrawRepository) {

        return new GetWalletWithdrawInputPort(walletWithdrawRepository);
    }

    @Bean
    public ProcessWalletDebitUseCase processWalletDebitInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            WalletServicePort walletServicePort,
            DomainEventPublisherPort eventPublisher) {

        return new ProcessWalletDebitInputPort(walletWithdrawRepository, walletServicePort, eventPublisher);
    }

    @Bean
    public ProcessPaymentUseCase processPaymentInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            PaymentProviderPort paymentProviderPort,
            PaymentSourceProviderPort paymentSourceProviderPort,
            DomainEventPublisherPort eventPublisher) {

        return new ProcessPaymentInputPort(walletWithdrawRepository, paymentProviderPort, paymentSourceProviderPort, eventPublisher);
    }
}