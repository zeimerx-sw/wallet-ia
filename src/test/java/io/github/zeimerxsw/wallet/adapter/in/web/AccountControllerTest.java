package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.application.service.AccountService;
import io.github.zeimerxsw.wallet.domain.exception.AccountNotFoundException;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AccountController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AccountService accountService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsService userDetailsService;

    @Test
    void createAccount_returns201WithId() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.createAccount()).thenReturn(accountId);

        mockMvc.perform(post("/accounts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(accountId.toString()));
    }

    @Test
    void getAccount_existingAccount_returns200WithBalance() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account account = new Account(AccountId.of(accountId), Money.of("250.00"));
        when(accountService.getAccount(accountId)).thenReturn(account);

        mockMvc.perform(get("/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.balance").value(250.00));
    }

    @Test
    void getAccount_nonExistent_returns404() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.getAccount(accountId))
                .thenThrow(new AccountNotFoundException(AccountId.of(accountId)));

        mockMvc.perform(get("/accounts/{id}", accountId))
                .andExpect(status().isNotFound());
    }
}
