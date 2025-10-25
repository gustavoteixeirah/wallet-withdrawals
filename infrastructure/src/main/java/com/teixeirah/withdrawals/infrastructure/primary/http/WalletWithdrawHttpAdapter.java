package com.teixeirah.withdrawals.infrastructure.primary.http;

import com.teixeirah.withdrawals.application.command.InitiateWalletWithdrawalCommand;
import com.teixeirah.withdrawals.application.input.GetWalletWithdrawInputPort;
import com.teixeirah.withdrawals.application.input.InitiateWalletWithdrawInputPort;
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

    private final InitiateWalletWithdrawalUseCase initiateWalletWithdrawInputPort;
    private final GetWalletWithdrawUseCase getWalletWithdrawInputPort;

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

    @GetMapping("/wallet_withdraw/{id}")
    ResponseEntity<WalletWithdrawResponse> getWalletWithdraw(@PathVariable UUID id) {

        log.info("Received get wallet withdraw request for id: {}", id);

        WalletWithdrawResponse response = getWalletWithdrawInputPort.execute(id);

        log.info("Wallet withdraw retrieved successfully: {}", response.id());

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