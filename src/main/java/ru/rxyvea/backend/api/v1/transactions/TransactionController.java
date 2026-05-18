package ru.rxyvea.backend.api.v1.transactions;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.rxyvea.backend.api.generated.TransactionsApi;
import ru.rxyvea.backend.api.generated.dto.CreateTransactionRequest;
import ru.rxyvea.backend.api.generated.dto.TransactionResponse;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionsApi {
    private final TransactionService transactionService;

    @Override
    public ResponseEntity<TransactionResponse> createTransaction(CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(request));
    }

    @Override
    public ResponseEntity<TransactionResponse> getTransaction(UUID id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }
}
