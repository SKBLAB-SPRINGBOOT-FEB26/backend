package ru.rxyvea.backend.model.strategy;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochRandomGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.uuid.UuidValueGenerator;

import java.util.UUID;

@SuppressWarnings("deprecation")
public class UUIDv7Strategy implements UUIDGenerationStrategy, UuidValueGenerator {
    private final TimeBasedEpochRandomGenerator generator = Generators.timeBasedEpochRandomGenerator();

    @Override
    public int getGeneratedVersion() {
        // a 'timestamp and random' strategy (rfc-9562)
        return 7;
    }

    @Override
    public UUID generateUUID(SharedSessionContractImplementor session) {
        return generateUuid(session);
    }

    @Override
    public UUID generateUuid(SharedSessionContractImplementor session) {
        return generator.generate();
    }
}
