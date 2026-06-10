package io.github.zeimerxsw.wallet.domain.exception;

import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(AccountId accountId, Money balance, Money requested) {
        super("Account " + accountId + " has balance " + balance + " but requested " + requested);
    }
}
