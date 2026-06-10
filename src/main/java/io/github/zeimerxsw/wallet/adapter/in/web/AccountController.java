package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.adapter.in.web.dto.AccountResponse;
import io.github.zeimerxsw.wallet.adapter.in.web.dto.TransactionResponse;
import io.github.zeimerxsw.wallet.application.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Accounts", description = "Wallet account management")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new zero-balance account")
    public AccountResponse createAccount() {
        UUID id = accountService.createAccount();
        return new AccountResponse(id, BigDecimal.ZERO);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account balance")
    public AccountResponse getAccount(@PathVariable UUID id) {
        var account = accountService.getAccount(id);
        return new AccountResponse(account.getId().getValue(), account.getBalance().getAmount());
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "List transaction history (paginated, default page size 20)")
    public Page<TransactionResponse> getTransactions(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return accountService.getTransactions(id, pageable)
                .map(t -> new TransactionResponse(t.id(), t.amount(), t.type().name(), t.createdAt()));
    }
}
