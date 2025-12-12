package com.ees.metadatastore;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DbMetadataStoreTest extends AbstractMetadataStoreContractTest {

    @Override
    protected MetadataStore createStore(TestMutableClock clock) throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:mds;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        ds.getConnection().prepareStatement("""
            CREATE TABLE IF NOT EXISTS metadata (
              id VARCHAR(255) PRIMARY KEY,
              payload BLOB NOT NULL,
              expires_at TIMESTAMP NULL
            )
            """).execute();
        return new DbMetadataStore(ds, "metadata", new JsonMetadataSerializer(), clock);
    }
}
