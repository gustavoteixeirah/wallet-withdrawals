
package com.teixeirah.withdrawals.application.usecase;

import com.teixeirah.withdrawals.application.command.ProcessWalletDebitCommand;

public interface ProcessWalletDebitUseCase {

    void execute(ProcessWalletDebitCommand command);

}