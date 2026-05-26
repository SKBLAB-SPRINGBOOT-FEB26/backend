package ru.rxyvea.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rxyvea.backend.model.Transaction;
import ru.rxyvea.backend.repository.projection.TransactionView;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<TransactionView> findViewById(UUID id);

    Optional<TransactionView> findViewByIdempotencyKey(String idempotencyKey);
}
