package ru.rxyvea.backend.api.v1.transactions;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rxyvea.backend.api.generated.dto.*;
import ru.rxyvea.backend.model.Account;
import ru.rxyvea.backend.model.Transaction;
import ru.rxyvea.backend.model.TransactionEntry;
import ru.rxyvea.backend.repository.AccountRepository;
import ru.rxyvea.backend.repository.TransactionRepository;
import ru.rxyvea.backend.repository.projection.TransactionView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest req) {
        if (req.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (req.getDebitAccountId().equals(req.getCreditAccountId())) {
            throw new IllegalArgumentException("Debit and credit accounts must differ");
        }

        var existing = transactionRepository.findViewByIdempotencyKey(req.getIdempotencyKey());
        if (existing.isPresent()) {
            return toResponse(existing.orElseThrow());
        }

        final var firstId = req.getDebitAccountId().compareTo(req.getCreditAccountId()) < 0
                ? req.getDebitAccountId() : req.getCreditAccountId();
        final var secondId = firstId.equals(req.getDebitAccountId())
                ? req.getCreditAccountId() : req.getDebitAccountId();

        Map<UUID, Account> locked = new HashMap<>();
        locked.put(firstId, accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + firstId)));
        locked.put(secondId, accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + secondId)));

        Account debit = locked.get(req.getDebitAccountId());
        Account credit = locked.get(req.getCreditAccountId());

        if (!debit.getCurrency().equals(req.getCurrency())
                || !credit.getCurrency().equals(req.getCurrency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }

        if (debit.getBalance().compareTo(req.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient funds: " + debit.getId());
        }

        debit.setBalance(debit.getBalance().subtract(req.getAmount()));
        credit.setBalance(credit.getBalance().add(req.getAmount()));

        final var tx = Transaction.builder()
                .referenceNumber("TX-" + UUID.randomUUID())
                .idempotencyKey(req.getIdempotencyKey())
                .transactionType(ru.rxyvea.backend.model.TransactionType.valueOf(req.getTransactionType().getValue()))
                .status(ru.rxyvea.backend.model.TransactionStatus.COMPLETED)
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

        try {
            final var saved = transactionRepository.saveAndFlush(tx);
            return toResponse(transactionRepository.findViewById(saved.getId()).orElseThrow());
        } catch (DataIntegrityViolationException e) {
            // in case parallel request w/ same idempotency_key arrived first
            return toResponse(transactionRepository.findViewByIdempotencyKey(req.getIdempotencyKey())
                    .orElseThrow(() -> e));
        }
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
