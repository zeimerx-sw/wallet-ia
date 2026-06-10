package io.github.zeimerxsw.wallet.application.port.in;

public record RegisterCommand(String email, String rawPassword) {}
