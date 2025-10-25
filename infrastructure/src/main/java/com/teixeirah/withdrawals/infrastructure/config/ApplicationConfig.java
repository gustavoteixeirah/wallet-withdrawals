package com.teixeirah.withdrawals.infrastructure.config;

import com.teixeirah.withdrawals.application.command.GetWalletWithdrawCommand;
import com.teixeirah.withdrawals.application.command.InitiateWalletWithdrawalCommand;
import com.teixeirah.withdrawals.application.command.ProcessPaymentCommand;
import com.teixeirah.withdrawals.application.command.ProcessWalletDebitCommand;
import com.teixeirah.withdrawals.application.input.GetWalletWithdrawInputPort;
import com.teixeirah.withdrawals.application.input.InitiateWalletWithdrawInputPort;
import com.teixeirah.withdrawals.application.input.ProcessPaymentInputPort;
import com.teixeirah.withdrawals.application.input.ProcessWalletDebitInputPort;
import com.teixeirah.withdrawals.application.response.InitiateWalletWithdrawalResponse;
import com.teixeirah.withdrawals.application.response.WalletWithdrawResponse;
import com.teixeirah.withdrawals.application.usecase.*;
import com.teixeirah.withdrawals.domain.events.DomainEventPublisherPort;
import com.teixeirah.withdrawals.domain.payments.PaymentProviderPort;
import com.teixeirah.withdrawals.domain.payments.PaymentSourceProviderPort;
import com.teixeirah.withdrawals.domain.wallet.service.WalletServicePort;
import com.teixeirah.withdrawals.domain.wallet.withdraw.WalletWithdrawRepository;
import com.teixeirah.withdrawals.infrastructure.wrappers.TransactionalUseCaseWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ApplicationConfig {

    @Bean
    public TransactionalUseCaseWrapper transactionalUseCaseWrapper(PlatformTransactionManager transactionManager) {
        return new TransactionalUseCaseWrapper(transactionManager);
    }

        @Bean
    public InitiateWalletWithdrawalUseCase initiateWalletWithdrawInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            DomainEventPublisherPort eventPublisher,
            TransactionalUseCaseWrapper transactionalWrapper) {

        InitiateWalletWithdrawalUseCase useCase =
            new InitiateWalletWithdrawInputPort(walletWithdrawRepository, eventPublisher);

        useCase = transactionalWrapper.wrap(useCase, new WrapperContext());

        return useCase;
    }

    @Bean
    public GetWalletWithdrawUseCase getWalletWithdrawInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            TransactionalUseCaseWrapper transactionalWrapper) {
        GetWalletWithdrawUseCase useCase =
            new GetWalletWithdrawInputPort(walletWithdrawRepository);

        useCase = transactionalWrapper.wrap(useCase, new WrapperContext());

        return useCase;
    }

    @Bean
    public ProcessWalletDebitUseCase processWalletDebitInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            WalletServicePort walletServicePort,
            DomainEventPublisherPort eventPublisher,
            TransactionalUseCaseWrapper transactionalWrapper) {
        ProcessWalletDebitUseCase useCase =
            new ProcessWalletDebitInputPort(walletWithdrawRepository, walletServicePort, eventPublisher);

        useCase = transactionalWrapper.wrap(useCase, new WrapperContext());

        return useCase;
    }

    @Bean
    public ProcessPaymentUseCase processPaymentInputPort(
            WalletWithdrawRepository walletWithdrawRepository,
            PaymentProviderPort paymentProviderPort,
            PaymentSourceProviderPort paymentSourceProviderPort,
            DomainEventPublisherPort eventPublisher,
            TransactionalUseCaseWrapper transactionalWrapper) {
        ProcessPaymentUseCase useCase =
            new ProcessPaymentInputPort(walletWithdrawRepository, paymentProviderPort, paymentSourceProviderPort, eventPublisher);

        useCase = transactionalWrapper.wrap(useCase, new WrapperContext());

        return useCase;
    }
}