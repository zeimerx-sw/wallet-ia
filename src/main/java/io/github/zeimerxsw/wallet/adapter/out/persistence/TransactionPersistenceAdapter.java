package io.github.zeimerxsw.wallet.adapter.out.persistence;

import io.github.zeimerxsw.wallet.adapter.out.persistence.mapper.TransactionMapper;
import io.github.zeimerxsw.wallet.application.model.TransactionDetail;
import io.github.zeimerxsw.wallet.application.port.out.TransactionQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransactionPersistenceAdapter implements TransactionQueryPort {

    private final TransactionJpaRepository transactionJpaRepository;
    private final TransactionMapper transactionMapper;

    public TransactionPersistenceAdapter(TransactionJpaRepository transactionJpaRepository, TransactionMapper transactionMapper) {
        this.transactionJpaRepository = transactionJpaRepository;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public Page<TransactionDetail> findByAccountId(UUID accountId, Pageable pageable) {
        return transactionJpaRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(transactionMapper::toDetail);
    }
}
