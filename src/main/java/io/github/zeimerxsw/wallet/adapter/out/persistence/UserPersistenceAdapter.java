package io.github.zeimerxsw.wallet.adapter.out.persistence;

import io.github.zeimerxsw.wallet.application.model.User;
import io.github.zeimerxsw.wallet.application.port.out.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        userJpaRepository.save(entity);
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(e -> new User(e.getId(), e.getEmail(), e.getPasswordHash()));
    }
}
