package com.teixeirah.withdrawals.infrastructure.primary.http;

import com.teixeirah.withdrawals.application.command.InitiateWalletWithdrawalCommand;
import com.teixeirah.withdrawals.application.input.InitiateWalletWithdrawInputPort;
import com.teixeirah.withdrawals.application.response.InitiateWalletWithdrawalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class WalletWithdrawHttpAdapter {

    private final InitiateWalletWithdrawInputPort initiateWalletWithdrawInputPort;

    @PostMapping("/wallet_withdraw")
    ResponseEntity<InitiateWalletWithdrawalResponse> initiateWalletWithdraw(
            @RequestBody InitiateWalletWithdrawRequest request) {

        log.info("Received wallet withdraw request: {}", request);

        InitiateWalletWithdrawalCommand command = new InitiateWalletWithdrawalCommand(
                request.userId(),
                request.amount(),
                request.recipientFirstName(),
                request.recipientLastName(),
                request.recipientRoutingNumber(),
                request.recipientNationalId(),
                request.recipientAccountNumber()
        );

        InitiateWalletWithdrawalResponse response = initiateWalletWithdrawInputPort.execute(command);

        log.info("Wallet withdraw initiated successfully: {}", response.transactionId());

        return ResponseEntity.ok(response);
    }

    record InitiateWalletWithdrawRequest(
            Long userId,
            BigDecimal amount,
            String recipientFirstName,
            String recipientLastName,
            String recipientRoutingNumber,
            String recipientNationalId,
            String recipientAccountNumber
    ) {}
}