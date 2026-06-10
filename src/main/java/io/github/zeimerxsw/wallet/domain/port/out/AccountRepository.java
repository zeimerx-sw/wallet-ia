package io.github.zeimerxsw.wallet.domain.port.out;

import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;

public interface AccountRepository {
    Account findById(AccountId id);
    Account findByIdForUpdate(AccountId id);
    void save(Account account);
}
