package io.github.zeimerxsw.wallet.application.service;

import io.github.zeimerxsw.wallet.application.port.in.TransferCommand;
import io.github.zeimerxsw.wallet.application.port.in.TransferUseCase;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import io.github.zeimerxsw.wallet.domain.service.TransferDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransferService implements TransferUseCase {

    private final AccountRepository accountRepository;
    private final TransferDomainService transferDomainService;

    public TransferService(AccountRepository accountRepository, TransferDomainService transferDomainService) {
        this.accountRepository = accountRepository;
        this.transferDomainService = transferDomainService;
    }

    @Override
    public void transfer(TransferCommand command) {
        AccountId sourceId = AccountId.of(command.sourceAccountId());
        AccountId targetId = AccountId.of(command.targetAccountId());
        Money amount = Money.of(command.amount());

        // Consistent lock ordering by UUID comparison prevents deadlocks
        boolean sourceFirst = sourceId.getValue().compareTo(targetId.getValue()) < 0;
        AccountId firstId = sourceFirst ? sourceId : targetId;
        AccountId secondId = sourceFirst ? targetId : sourceId;

        Account first = accountRepository.findByIdForUpdate(firstId);
        Account second = accountRepository.findByIdForUpdate(secondId);

        Account source = firstId.equals(sourceId) ? first : second;
        Account target = firstId.equals(targetId) ? first : second;

        transferDomainService.transfer(source, target, amount);

        accountRepository.save(source);
        accountRepository.save(target);
    }
}
