package io.github.zeimerxsw.wallet.domain.exception;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException() {
        super("Source and target accounts must be different");
    }
}
