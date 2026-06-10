package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.adapter.in.web.dto.CreateTransferRequest;
import io.github.zeimerxsw.wallet.application.port.in.TransferCommand;
import io.github.zeimerxsw.wallet.application.port.in.TransferUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
@Tag(name = "Transfers", description = "PIX-like transfers between accounts")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferUseCase transferUseCase;

    public TransferController(TransferUseCase transferUseCase) {
        this.transferUseCase = transferUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Transfer funds between two accounts")
    public void transfer(@RequestBody @Valid CreateTransferRequest request) {
        transferUseCase.transfer(new TransferCommand(
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount()));
    }
}
