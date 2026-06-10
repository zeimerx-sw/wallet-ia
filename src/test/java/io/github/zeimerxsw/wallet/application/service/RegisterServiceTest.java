package io.github.zeimerxsw.wallet.application.service;

import io.github.zeimerxsw.wallet.application.model.User;
import io.github.zeimerxsw.wallet.application.port.in.RegisterCommand;
import io.github.zeimerxsw.wallet.application.port.in.RegisterResult;
import io.github.zeimerxsw.wallet.application.port.out.UserRepository;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock PasswordEncoder passwordEncoder;

    RegisterService registerService;

    @BeforeEach
    void setUp() {
        registerService = new RegisterService(userRepository, accountRepository, passwordEncoder);
    }

    @Test
    void register_hashesPasswordBeforeSave() {
        when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        registerService.register(new RegisterCommand("user@example.com", "rawPassword"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(userCaptor.getValue().getPasswordHash()).doesNotContain("rawPassword");
    }

    @Test
    void register_createsAccountWithZeroBalance() {
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        registerService.register(new RegisterCommand("user@example.com", "password"));

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getBalance()).isEqualTo(Money.zero());
    }

    @Test
    void register_returnsNonNullUserIdAndAccountId() {
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterResult result = registerService.register(new RegisterCommand("user@example.com", "password"));

        assertThat(result.userId()).isNotNull();
        assertThat(result.accountId()).isNotNull();
    }
}
