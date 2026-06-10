package io.github.zeimerxsw.wallet.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class AccountId {
    private final UUID value;

    public AccountId(UUID value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public static AccountId generate() { return new AccountId(UUID.randomUUID()); }
    public static AccountId of(UUID value) { return new AccountId(value); }
    public static AccountId of(String value) { return new AccountId(UUID.fromString(value)); }

    public UUID getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountId a)) return false;
        return value.equals(a.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value.toString(); }
}
