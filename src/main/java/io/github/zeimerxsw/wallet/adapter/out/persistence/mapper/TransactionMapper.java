package io.github.zeimerxsw.wallet.adapter.out.persistence.mapper;

import io.github.zeimerxsw.wallet.adapter.out.persistence.TransactionJpaEntity;
import io.github.zeimerxsw.wallet.application.model.TransactionDetail;
import io.github.zeimerxsw.wallet.domain.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionJpaEntity toJpa(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.setId(transaction.getId());
        entity.setAccountId(transaction.getAccountId().getValue());
        entity.setAmount(transaction.getAmount().getAmount());
        entity.setType(transaction.getType());
        entity.setCreatedAt(transaction.getCreatedAt());
        return entity;
    }

    public TransactionDetail toDetail(TransactionJpaEntity entity) {
        return new TransactionDetail(entity.getId(), entity.getAmount(), entity.getType(), entity.getCreatedAt());
    }
}
