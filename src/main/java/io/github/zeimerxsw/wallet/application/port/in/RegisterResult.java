package io.github.zeimerxsw.wallet.application.port.in;

import java.util.UUID;

public record RegisterResult(UUID userId, UUID accountId) {}
