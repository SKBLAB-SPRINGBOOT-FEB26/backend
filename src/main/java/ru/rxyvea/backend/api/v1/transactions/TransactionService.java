package ru.rxyvea.backend.api.v1.transactions;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rxyvea.backend.api.generated.dto.CreateTransactionRequest;
import ru.rxyvea.backend.api.generated.dto.EntryResponse;
import ru.rxyvea.backend.api.generated.dto.EntryType;
import ru.rxyvea.backend.api.generated.dto.TransactionResponse;
import ru.rxyvea.backend.api.generated.dto.TransactionStatus;
import ru.rxyvea.backend.api.generated.dto.TransactionType;
import ru.rxyvea.backend.model.Transaction;
import ru.rxyvea.backend.model.TransactionEntry;
import ru.rxyvea.backend.repository.AccountRepository;
import ru.rxyvea.backend.repository.TransactionRepository;
import ru.rxyvea.backend.repository.projection.TransactionView;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest req) {
        final var debit = accountRepository.findById(req.getDebitAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Debit account not found"));
        final var credit = accountRepository.findById(req.getCreditAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Credit account not found"));

        final var tx = Transaction.builder()
                .referenceNumber("TX-" + UUID.randomUUID())
                .idempotencyKey(req.getIdempotencyKey())
                .transactionType(ru.rxyvea.backend.model.TransactionType.valueOf(req.getTransactionType().getValue()))
                .status(ru.rxyvea.backend.model.TransactionStatus.PENDING)
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .description(req.getDescription())
                .build();

        tx.addEntry(TransactionEntry.builder()
                .account(debit)
                .entryType(ru.rxyvea.backend.model.EntryType.DEBIT)
                .amount(req.getAmount())
                .build());

        tx.addEntry(TransactionEntry.builder()
                .account(credit)
                .entryType(ru.rxyvea.backend.model.EntryType.CREDIT)
                .amount(req.getAmount())
                .build());

        final var saved = transactionRepository.save(tx);
        return toResponse(transactionRepository.findViewById(saved.getId())
                .orElseThrow(() -> new IllegalStateException("Saved transaction not visible")));
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID id) {
        final var view = transactionRepository.findViewById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction " + id + " not found"));
        return toResponse(view);
    }

    private static TransactionResponse toResponse(TransactionView v) {
        final var entries = v.getEntries().stream()
                .map(e -> new EntryResponse(
                        e.getId(),
                        EntryType.valueOf(e.getEntryType().name()),
                        e.getAmount(),
                        e.getCreatedAt(),
                        e.getAccount().getId()
                ))
                .toList();

        final var resp = new TransactionResponse(
                v.getId(),
                v.getReferenceNumber(),
                v.getIdempotencyKey(),
                TransactionType.valueOf(v.getTransactionType().name()),
                TransactionStatus.valueOf(v.getStatus().name()),
                v.getAmount(),
                v.getCurrency(),
                v.getCreatedAt(),
                entries
        );
        resp.setDescription(v.getDescription());
        resp.setCompletedAt(v.getCompletedAt());
        return resp;
    }
}
