package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.application.service.AccountService;
import io.github.zeimerxsw.wallet.domain.exception.AccountNotFoundException;
import io.github.zeimerxsw.wallet.domain.exception.InsufficientFundsException;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AccountController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ControllerAdviceTest {

    @Autowired MockMvc mockMvc;
    @MockBean AccountService accountService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsService userDetailsService;

    @Test
    void accountNotFound_returns404WithCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(id))
                .thenThrow(new AccountNotFoundException(AccountId.of(id)));

        mockMvc.perform(get("/accounts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void insufficientFunds_returns422WithCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(id))
                .thenThrow(new InsufficientFundsException(AccountId.of(id), Money.of("10.00"), Money.of("100.00")));

        mockMvc.perform(get("/accounts/{id}", id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void illegalArgument_returns400WithCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(id))
                .thenThrow(new IllegalArgumentException("invalid uuid"));

        mockMvc.perform(get("/accounts/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
