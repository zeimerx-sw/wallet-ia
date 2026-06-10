package io.github.zeimerxsw.wallet.application.port.in;

public interface RegisterUseCase {
    RegisterResult register(RegisterCommand command);
}
