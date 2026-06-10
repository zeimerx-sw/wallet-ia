package io.github.zeimerxsw.wallet.domain.model;

import io.github.zeimerxsw.wallet.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AccountTest {

    @Test
    void credit_updatesBalanceAndRecordsTransaction() {
        Account account = new Account(AccountId.generate(), Money.zero());
        account.credit(Money.of("100.00"));
        assertThat(account.getBalance()).isEqualTo(Money.of("100.00"));
        assertThat(account.getDomainTransactions()).hasSize(1);
        assertThat(account.getDomainTransactions().get(0).getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(account.getDomainTransactions().get(0).getAmount()).isEqualTo(Money.of("100.00"));
    }

    @Test
    void debit_withSufficientFunds_updatesBalanceAndRecordsTransaction() {
        Account account = new Account(AccountId.generate(), Money.of("100.00"));
        account.debit(Money.of("40.00"));
        assertThat(account.getBalance()).isEqualTo(Money.of("60.00"));
        assertThat(account.getDomainTransactions()).hasSize(1);
        assertThat(account.getDomainTransactions().get(0).getType()).isEqualTo(TransactionType.DEBIT);
    }

    @Test
    void debit_withInsufficientFunds_throwsAndLeavesBalanceUnchanged() {
        Account account = new Account(AccountId.generate(), Money.of("50.00"));
        assertThatThrownBy(() -> account.debit(Money.of("100.00")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(account.getBalance()).isEqualTo(Money.of("50.00"));
        assertThat(account.getDomainTransactions()).isEmpty();
    }

    @Test
    void debit_exactBalance_succeeds() {
        Account account = new Account(AccountId.generate(), Money.of("50.00"));
        account.debit(Money.of("50.00"));
        assertThat(account.getBalance()).isEqualTo(Money.zero());
    }

    @Test
    void multipleOperations_accumulateTransactions() {
        Account account = new Account(AccountId.generate(), Money.of("100.00"));
        account.debit(Money.of("30.00"));
        account.credit(Money.of("20.00"));
        assertThat(account.getBalance()).isEqualTo(Money.of("90.00"));
        assertThat(account.getDomainTransactions()).hasSize(2);
    }
}
