package io.github.zeimerxsw.wallet.adapter.out.persistence;

import io.github.zeimerxsw.wallet.adapter.out.persistence.mapper.AccountMapper;
import io.github.zeimerxsw.wallet.adapter.out.persistence.mapper.TransactionMapper;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({AccountMapper.class, TransactionMapper.class})
class AccountPersistenceAdapterTest {

    @Autowired AccountJpaRepository accountJpaRepository;
    @Autowired TransactionJpaRepository transactionJpaRepository;
    @Autowired AccountMapper accountMapper;
    @Autowired TransactionMapper transactionMapper;

    AccountPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccountPersistenceAdapter(
                accountJpaRepository, transactionJpaRepository, accountMapper, transactionMapper);
    }

    @Test
    void save_newAccount_persistsToDatabase() {
        Account account = new Account(AccountId.generate(), Money.of("100.00"));
        adapter.save(account);
        assertThat(accountJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void save_persistsTransactionsViaTransactionRepository() {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setBalance(new BigDecimal("100.00"));
        accountJpaRepository.save(entity);

        Account account = adapter.findById(AccountId.of(entity.getId()));
        account.debit(Money.of("30.00"));
        adapter.save(account);

        List<TransactionJpaEntity> transactions = transactionJpaRepository.findAll();
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void findById_nonExistent_throwsAccountNotFoundException() {
        var id = AccountId.of(UUID.randomUUID());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.findById(id))
                .isInstanceOf(io.github.zeimerxsw.wallet.domain.exception.AccountNotFoundException.class);
    }
}
