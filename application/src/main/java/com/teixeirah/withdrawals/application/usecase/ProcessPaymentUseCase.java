package com.teixeirah.withdrawals.application.usecase;

import com.teixeirah.withdrawals.application.command.ProcessPaymentCommand;

public interface ProcessPaymentUseCase {

    void execute(ProcessPaymentCommand command);
}
