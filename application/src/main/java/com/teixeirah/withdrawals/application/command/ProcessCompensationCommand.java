package com.teixeirah.withdrawals.application.command;

import java.util.UUID;

public record ProcessCompensationCommand(UUID withdrawalId) {
}