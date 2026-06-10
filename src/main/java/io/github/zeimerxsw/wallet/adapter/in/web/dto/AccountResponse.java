package io.github.zeimerxsw.wallet.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, BigDecimal balance) {}
