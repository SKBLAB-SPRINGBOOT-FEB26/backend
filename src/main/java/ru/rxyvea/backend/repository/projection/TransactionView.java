package ru.rxyvea.backend.repository.projection;

import ru.rxyvea.backend.model.EntryType;
import ru.rxyvea.backend.model.TransactionStatus;
import ru.rxyvea.backend.model.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionView {
    UUID getId();
    String getReferenceNumber();
    String getIdempotencyKey();
    TransactionType getTransactionType();
    TransactionStatus getStatus();
    BigDecimal getAmount();
    String getCurrency();
    String getDescription();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getCompletedAt();
    List<EntryView> getEntries();

    interface EntryView {
        UUID getId();
        EntryType getEntryType();
        BigDecimal getAmount();
        OffsetDateTime getCreatedAt();
        AccountRef getAccount();
    }

    interface AccountRef {
        UUID getId();
        String getAccountNumber();
        String getCurrency();
    }
}
