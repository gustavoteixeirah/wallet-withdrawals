package com.teixeirah.withdrawals.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WalletWithdrawalsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletWithdrawalsApplication.class, args);
    }
}