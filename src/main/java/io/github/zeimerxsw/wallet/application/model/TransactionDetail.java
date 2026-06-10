package io.github.zeimerxsw.wallet.application.model;

import io.github.zeimerxsw.wallet.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDetail(UUID id, BigDecimal amount, TransactionType type, Instant createdAt) {}
