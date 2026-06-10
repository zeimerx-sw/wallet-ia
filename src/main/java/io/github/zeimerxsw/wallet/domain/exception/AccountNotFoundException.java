package io.github.zeimerxsw.wallet.domain.exception;

import io.github.zeimerxsw.wallet.domain.model.AccountId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(AccountId id) {
        super("Account not found: " + id);
    }
}
