package com.teixeirah.withdrawals.application.usecase;

import com.teixeirah.withdrawals.application.command.ProcessCompensationCommand;

public interface ProcessCompensationUseCase {

    void execute(final ProcessCompensationCommand command);

}