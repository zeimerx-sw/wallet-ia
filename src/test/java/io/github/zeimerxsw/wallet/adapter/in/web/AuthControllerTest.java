package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.application.port.in.RegisterCommand;
import io.github.zeimerxsw.wallet.application.port.in.RegisterResult;
import io.github.zeimerxsw.wallet.application.port.in.RegisterUseCase;
import io.github.zeimerxsw.wallet.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean RegisterUseCase registerUseCase;
    @MockBean AuthenticationManager authenticationManager;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsService userDetailsService;

    @Test
    void register_validRequest_returns201WithIds() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(registerUseCase.register(any(RegisterCommand.class)))
                .thenReturn(new RegisterResult(userId, accountId));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"rawPassword\":\"secure123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"rawPassword\":\"secure123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void token_validCredentials_returns200WithToken() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("user@example.com", null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken("user@example.com")).thenReturn("signed.jwt.token");

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secure123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt.token"));
    }
}
