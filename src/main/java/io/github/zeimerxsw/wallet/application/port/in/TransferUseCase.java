package io.github.zeimerxsw.wallet.application.port.in;

public interface TransferUseCase {
    void transfer(TransferCommand command);
}
