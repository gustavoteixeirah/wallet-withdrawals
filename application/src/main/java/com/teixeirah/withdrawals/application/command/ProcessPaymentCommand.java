package com.teixeirah.withdrawals.application.command;

import java.util.UUID;

public record ProcessPaymentCommand(UUID withdrawalId) {
}
