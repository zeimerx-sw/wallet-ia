package io.github.zeimerxsw.wallet.adapter.out.persistence;

import io.github.zeimerxsw.wallet.adapter.out.persistence.mapper.AccountMapper;
import io.github.zeimerxsw.wallet.adapter.out.persistence.mapper.TransactionMapper;
import io.github.zeimerxsw.wallet.domain.exception.AccountNotFoundException;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountPersistenceAdapter implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;
    private final TransactionJpaRepository transactionJpaRepository;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;

    public AccountPersistenceAdapter(
            AccountJpaRepository accountJpaRepository,
            TransactionJpaRepository transactionJpaRepository,
            AccountMapper accountMapper,
            TransactionMapper transactionMapper) {
        this.accountJpaRepository = accountJpaRepository;
        this.transactionJpaRepository = transactionJpaRepository;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public Account findById(AccountId id) {
        return accountJpaRepository.findById(id.getValue())
                .map(accountMapper::toDomain)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Override
    public Account findByIdForUpdate(AccountId id) {
        return accountJpaRepository.findByIdForUpdate(id.getValue())
                .map(accountMapper::toDomain)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Override
    public void save(Account account) {
        AccountJpaEntity entity = accountJpaRepository.findById(account.getId().getValue())
                .orElse(new AccountJpaEntity());
        entity.setId(account.getId().getValue());
        entity.setBalance(account.getBalance().getAmount());
        accountJpaRepository.save(entity);

        List<TransactionJpaEntity> newTransactions = account.getDomainTransactions()
                .stream()
                .map(transactionMapper::toJpa)
                .toList();
        if (!newTransactions.isEmpty()) {
            transactionJpaRepository.saveAll(newTransactions);
        }
    }
}
