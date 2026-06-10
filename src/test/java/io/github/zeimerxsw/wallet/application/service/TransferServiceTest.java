package io.github.zeimerxsw.wallet.application.service;

import io.github.zeimerxsw.wallet.application.port.in.TransferCommand;
import io.github.zeimerxsw.wallet.domain.exception.InsufficientFundsException;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import io.github.zeimerxsw.wallet.domain.service.TransferDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock AccountRepository accountRepository;

    TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(accountRepository, new TransferDomainService());
    }

    @Test
    void transfer_withSufficientFunds_updatesBalancesAndSavesBoth() {
        AccountId sourceId = AccountId.generate();
        AccountId targetId = AccountId.generate();
        Account source = new Account(sourceId, Money.of("100.00"));
        Account target = new Account(targetId, Money.zero());

        when(accountRepository.findByIdForUpdate(any())).thenAnswer(inv -> {
            AccountId id = inv.getArgument(0);
            return id.equals(sourceId) ? source : target;
        });

        transferService.transfer(new TransferCommand(sourceId.getValue(), targetId.getValue(), new BigDecimal("40.00")));

        assertThat(source.getBalance()).isEqualTo(Money.of("60.00"));
        assertThat(target.getBalance()).isEqualTo(Money.of("40.00"));
        verify(accountRepository, times(2)).save(any());
    }

    @Test
    void transfer_withInsufficientFunds_throwsAndDoesNotSave() {
        AccountId sourceId = AccountId.generate();
        AccountId targetId = AccountId.generate();
        Account source = new Account(sourceId, Money.of("10.00"));
        Account target = new Account(targetId, Money.zero());

        when(accountRepository.findByIdForUpdate(any())).thenAnswer(inv -> {
            AccountId id = inv.getArgument(0);
            return id.equals(sourceId) ? source : target;
        });

        assertThatThrownBy(() ->
                transferService.transfer(new TransferCommand(sourceId.getValue(), targetId.getValue(), new BigDecimal("100.00")))
        ).isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any());
    }
}
