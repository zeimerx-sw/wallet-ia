package io.github.zeimerxsw.wallet.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(UUID id, BigDecimal amount, String type, Instant createdAt) {}
