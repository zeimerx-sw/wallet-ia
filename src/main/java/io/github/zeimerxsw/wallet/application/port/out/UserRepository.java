package io.github.zeimerxsw.wallet.application.port.out;

import io.github.zeimerxsw.wallet.application.model.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
}
