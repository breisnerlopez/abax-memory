package com.abax.memory.test;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Test profile for H2 in-memory database testing.
 *
 * <p>Disables Flyway (whose migrations contain PostgreSQL-specific
 * partial-index syntax unsupported by H2) and uses Hibernate's
 * {@code drop-and-create} for schema generation instead.</p>
 *
 * <p>This profile is necessary because the production Flyway
 * migrations (V2__create_memory_fragments.sql line 60) use
 * {@code WHERE deleted_at IS NULL} partial index syntax that is
 * not supported by H2's PostgreSQL compatibility mode.</p>
 *
 * <p>Production integration tests should use the default profile
 * with Testcontainers + PostgreSQL.</p>
 */
public class H2TestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.flyway.migrate-at-start", "false",
                "quarkus.hibernate-orm.database.generation", "drop-and-create"
        );
    }
}
