package io.github.zeimerxsw.wallet.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount) {}
