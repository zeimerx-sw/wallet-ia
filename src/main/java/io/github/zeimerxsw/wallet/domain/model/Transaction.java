package io.github.zeimerxsw.wallet.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Transaction {
    private final UUID id;
    private final AccountId accountId;
    private final Money amount;
    private final TransactionType type;
    private final Instant createdAt;

    public Transaction(UUID id, AccountId accountId, Money amount, TransactionType type, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.amount = Objects.requireNonNull(amount);
        this.type = Objects.requireNonNull(type);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public UUID getId() { return id; }
    public AccountId getAccountId() { return accountId; }
    public Money getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}
