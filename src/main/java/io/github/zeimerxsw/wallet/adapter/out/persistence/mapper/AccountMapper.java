package io.github.zeimerxsw.wallet.adapter.out.persistence.mapper;

import io.github.zeimerxsw.wallet.adapter.out.persistence.AccountJpaEntity;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public Account toDomain(AccountJpaEntity entity) {
        return new Account(AccountId.of(entity.getId()), Money.of(entity.getBalance()));
    }

    public AccountJpaEntity toJpa(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(account.getId().getValue());
        entity.setBalance(account.getBalance().getAmount());
        return entity;
    }
}
