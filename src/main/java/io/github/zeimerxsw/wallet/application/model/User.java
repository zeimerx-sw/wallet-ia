package io.github.zeimerxsw.wallet.application.model;

import java.util.UUID;

public class User {
    private final UUID id;
    private final String email;
    private final String passwordHash;

    public User(UUID id, String email, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}
