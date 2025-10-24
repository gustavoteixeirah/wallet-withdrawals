package com.teixeirah.withdrawals.application.command;

import java.math.BigDecimal;

public record InitiateWalletWithdrawalCommand(Long userId,
                                              BigDecimal amount,
                                              String recipientFirstName,
                                              String recipientLastName,
                                              String recipientRoutingNumber,
                                              String recipientNationalId,
                                              String recipientAccountNumber) {
}