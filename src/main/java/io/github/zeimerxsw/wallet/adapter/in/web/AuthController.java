package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.adapter.in.web.dto.LoginRequest;
import io.github.zeimerxsw.wallet.adapter.in.web.dto.LoginResponse;
import io.github.zeimerxsw.wallet.adapter.in.web.dto.RegisterRequest;
import io.github.zeimerxsw.wallet.adapter.in.web.dto.RegisterResponse;
import io.github.zeimerxsw.wallet.application.port.in.RegisterCommand;
import io.github.zeimerxsw.wallet.application.port.in.RegisterResult;
import io.github.zeimerxsw.wallet.application.port.in.RegisterUseCase;
import io.github.zeimerxsw.wallet.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Register and obtain JWT tokens")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(RegisterUseCase registerUseCase, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.registerUseCase = registerUseCase;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user and create a wallet account")
    public RegisterResponse register(@RequestBody @Valid RegisterRequest request) {
        RegisterResult result = registerUseCase.register(new RegisterCommand(request.email(), request.rawPassword()));
        return new RegisterResponse(result.userId(), result.accountId());
    }

    @PostMapping("/token")
    @Operation(summary = "Authenticate and receive a JWT token")
    public LoginResponse token(@RequestBody @Valid LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        return new LoginResponse(jwtTokenProvider.generateToken(auth.getName()));
    }
}
