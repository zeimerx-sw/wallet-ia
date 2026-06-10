package io.github.zeimerxsw.wallet.domain.model;

import io.github.zeimerxsw.wallet.domain.exception.InsufficientFundsException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Account {
    private final AccountId id;
    private Money balance;
    private final List<Transaction> domainTransactions = new ArrayList<>();

    public Account(AccountId id, Money balance) {
        this.id = Objects.requireNonNull(id);
        this.balance = Objects.requireNonNull(balance);
    }

    public void credit(Money amount) {
        this.balance = this.balance.add(amount);
        domainTransactions.add(new Transaction(UUID.randomUUID(), this.id, amount, TransactionType.CREDIT, Instant.now()));
    }

    public void debit(Money amount) {
        if (amount.isGreaterThan(balance)) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        this.balance = this.balance.subtract(amount);
        domainTransactions.add(new Transaction(UUID.randomUUID(), this.id, amount, TransactionType.DEBIT, Instant.now()));
    }

    public AccountId getId() { return id; }
    public Money getBalance() { return balance; }
    public List<Transaction> getDomainTransactions() { return Collections.unmodifiableList(domainTransactions); }
}
