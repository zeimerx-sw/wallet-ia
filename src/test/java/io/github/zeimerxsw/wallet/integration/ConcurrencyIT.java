package io.github.zeimerxsw.wallet.integration;

import io.github.zeimerxsw.wallet.adapter.out.persistence.AccountJpaEntity;
import io.github.zeimerxsw.wallet.adapter.out.persistence.AccountJpaRepository;
import io.github.zeimerxsw.wallet.adapter.out.persistence.TransactionJpaRepository;
import io.github.zeimerxsw.wallet.application.port.in.TransferCommand;
import io.github.zeimerxsw.wallet.application.port.in.TransferUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("integration")
class ConcurrencyIT {

    @Autowired TransferUseCase transferUseCase;
    @Autowired AccountJpaRepository accountJpaRepository;
    @Autowired TransactionJpaRepository transactionJpaRepository;

    UUID accountAId;
    UUID accountBId;

    @BeforeEach
    void setUp() {
        transactionJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();

        AccountJpaEntity accountA = new AccountJpaEntity();
        accountA.setId(UUID.randomUUID());
        accountA.setBalance(new BigDecimal("1000.00"));
        accountJpaRepository.save(accountA);
        accountAId = accountA.getId();

        AccountJpaEntity accountB = new AccountJpaEntity();
        accountB.setId(UUID.randomUUID());
        accountB.setBalance(new BigDecimal("1000.00"));
        accountJpaRepository.save(accountB);
        accountBId = accountB.getId();
    }

    @Test
    void concurrentTransfers_maintainConsistentTotalBalance() throws Exception {
        int threads = 10;
        BigDecimal transferAmount = new BigDecimal("10.00");
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final boolean aToB = i < 5;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    if (aToB) {
                        transferUseCase.transfer(new TransferCommand(accountAId, accountBId, transferAmount));
                    } else {
                        transferUseCase.transfer(new TransferCommand(accountBId, accountAId, transferAmount));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) f.get();
        executor.shutdown();

        BigDecimal balanceA = accountJpaRepository.findById(accountAId).orElseThrow().getBalance();
        BigDecimal balanceB = accountJpaRepository.findById(accountBId).orElseThrow().getBalance();

        assertThat(balanceA.add(balanceB)).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(transactionJpaRepository.count()).isEqualTo(threads * 2L);
    }
}
