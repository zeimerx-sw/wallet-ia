package io.github.zeimerxsw.wallet.application.service;

import io.github.zeimerxsw.wallet.application.model.User;
import io.github.zeimerxsw.wallet.application.port.in.RegisterCommand;
import io.github.zeimerxsw.wallet.application.port.in.RegisterResult;
import io.github.zeimerxsw.wallet.application.port.in.RegisterUseCase;
import io.github.zeimerxsw.wallet.application.port.out.UserRepository;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class RegisterService implements RegisterUseCase {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResult register(RegisterCommand command) {
        String hashedPassword = passwordEncoder.encode(command.rawPassword());
        User user = new User(UUID.randomUUID(), command.email(), hashedPassword);
        userRepository.save(user);

        Account account = new Account(AccountId.generate(), Money.zero());
        accountRepository.save(account);

        return new RegisterResult(user.getId(), account.getId().getValue());
    }
}
