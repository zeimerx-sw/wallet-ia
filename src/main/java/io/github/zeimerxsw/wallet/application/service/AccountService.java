package io.github.zeimerxsw.wallet.application.service;

import io.github.zeimerxsw.wallet.application.model.TransactionDetail;
import io.github.zeimerxsw.wallet.application.port.out.TransactionQueryPort;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionQueryPort transactionQueryPort;

    public AccountService(AccountRepository accountRepository, TransactionQueryPort transactionQueryPort) {
        this.accountRepository = accountRepository;
        this.transactionQueryPort = transactionQueryPort;
    }

    public UUID createAccount() {
        Account account = new Account(AccountId.generate(), Money.zero());
        accountRepository.save(account);
        return account.getId().getValue();
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID id) {
        return accountRepository.findById(AccountId.of(id));
    }

    @Transactional(readOnly = true)
    public Page<TransactionDetail> getTransactions(UUID accountId, Pageable pageable) {
        return transactionQueryPort.findByAccountId(accountId, pageable);
    }
}
