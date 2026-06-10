package io.github.zeimerxsw.wallet.domain.service;

import io.github.zeimerxsw.wallet.domain.exception.SameAccountTransferException;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class TransferDomainService {
    public void transfer(Account source, Account target, Money amount) {
        if (source.getId().equals(target.getId())) {
            throw new SameAccountTransferException();
        }
        source.debit(amount);
        target.credit(amount);
    }
}
