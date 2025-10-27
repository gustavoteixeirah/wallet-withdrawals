package com.teixeirah.withdrawals.infrastructure.primary.http;

import com.teixeirah.withdrawals.domain.wallet.withdraw.exceptions.WalletWithdrawNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class WalletWithdrawExceptionHandler {

    @ExceptionHandler(WalletWithdrawNotFoundException.class)
    public ResponseEntity<Void> handleWalletWithdrawNotFound(WalletWithdrawNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointer(NullPointerException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    public record ErrorResponse(String message) {}
}