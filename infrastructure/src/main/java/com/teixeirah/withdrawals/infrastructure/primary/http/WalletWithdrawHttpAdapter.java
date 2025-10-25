package com.teixeirah.withdrawals.infrastructure.primary.http;

import com.teixeirah.withdrawals.application.command.GetWalletWithdrawCommand;
import com.teixeirah.withdrawals.application.command.InitiateWalletWithdrawalCommand;
import com.teixeirah.withdrawals.application.response.InitiateWalletWithdrawalResponse;
import com.teixeirah.withdrawals.application.response.WalletWithdrawResponse;
import com.teixeirah.withdrawals.application.usecase.GetWalletWithdrawUseCase;
import com.teixeirah.withdrawals.application.usecase.InitiateWalletWithdrawalUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class WalletWithdrawHttpAdapter {

    private final InitiateWalletWithdrawalUseCase initiateWalletWithdrawUseCase;
    private final GetWalletWithdrawUseCase getWalletWithdrawUseCase;

    @PostMapping("/wallet_withdraw")
    ResponseEntity<InitiateWalletWithdrawalResponse> initiateWalletWithdraw(
            @RequestBody InitiateWalletWithdrawRequest request) {

        // Structured log for incoming request (avoid logging full request object)
        log.atInfo()
           .addKeyValue("path", "/api/v1/wallet_withdraw")
           .addKeyValue("userId", request.userId())
           .addKeyValue("amount", request.amount())
           .log("received_wallet_withdraw_request");

        InitiateWalletWithdrawalCommand command = new InitiateWalletWithdrawalCommand(
                request.userId(),
                request.amount(),
                request.recipientFirstName(),
                request.recipientLastName(),
                request.recipientRoutingNumber(),
                request.recipientNationalId(),
                request.recipientAccountNumber()
        );

        InitiateWalletWithdrawalResponse response = initiateWalletWithdrawUseCase.execute(command);

        log.atInfo()
           .addKeyValue("transactionId", response.transactionId())
           .log("wallet_withdraw_initiated");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallet_withdraw/{id}")
    ResponseEntity<WalletWithdrawResponse> getWalletWithdraw(@PathVariable UUID id) {
        log.atInfo()
           .addKeyValue("path", "/api/v1/wallet_withdraw/{id}")
           .addKeyValue("id", id)
           .log("get_wallet_withdraw_request");

        GetWalletWithdrawCommand command = new GetWalletWithdrawCommand(id);
        WalletWithdrawResponse response = getWalletWithdrawUseCase.execute(command);

        log.atInfo()
           .addKeyValue("id", response.id())
           .log("wallet_withdraw_retrieved");
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
    ) {
    }
}