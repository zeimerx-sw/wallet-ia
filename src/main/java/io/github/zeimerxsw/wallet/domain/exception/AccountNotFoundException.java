package io.github.zeimerxsw.wallet.domain.exception;

import io.github.zeimerxsw.wallet.domain.model.AccountId;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(AccountId id) {
        super("Account not found: " + id);
    }
}
