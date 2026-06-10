package io.github.zeimerxsw.wallet.application.port.out;

import io.github.zeimerxsw.wallet.application.model.TransactionDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransactionQueryPort {
    Page<TransactionDetail> findByAccountId(UUID accountId, Pageable pageable);
}
