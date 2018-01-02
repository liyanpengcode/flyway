/*
 * Copyright 2010-2018 Boxfuse GmbH
 *
 * INTERNAL RELEASE. ALL RIGHTS RESERVED.
 *
 * Must
 * be
 * exactly
 * 13 lines
 * to match
 * community
 * edition
 * license
 * length.
 */
package org.flywaydb.core.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationType;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.database.Database;
import org.flywaydb.core.internal.database.DatabaseFactory;
import org.flywaydb.core.internal.util.jdbc.JdbcTemplate;
import org.flywaydb.core.internal.util.jdbc.JdbcUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test to demonstrate the migration functionality using H2.
 */
@SuppressWarnings({"JavaDoc"})
public abstract class ConcurrentMigrationTestCase {
    private static final Log LOG = LogFactory.getLog(ConcurrentMigrationTestCase.class);

    @Rule
    public Timeout globalTimeout = new Timeout(180, TimeUnit.SECONDS);

    /**
     * The number of threads to use in this test.
     */
    private static final int NUM_THREADS = 10;

    /**
     * The quoted schema placeholder for the tests.
     */
    private String schemaQuoted;

    /**
     * Error message in case the concurrent test has failed.
     */
    private String error;

    /**
     * The datasource to use for concurrent migration tests.
     */
    private DataSource concurrentMigrationDataSource;

    /**
     * The instance under test.
     */
    private Flyway flyway;
    private String schemaName = getSchemaName();

    protected String getSchemaName() {
        return "concurrent_test";
    }

    @Before
    public void setUp() throws Exception {
        ensureTestEnabled();

        concurrentMigrationDataSource = createDataSource();

        flyway = createFlyway();

        final Database database = DatabaseFactory.createDatabase(flyway, false, null);
        schemaQuoted = database.quote(schemaName);

        flyway.clean();

        if (needsBaseline()) {
            flyway.baseline();
        }
    }

    protected void ensureTestEnabled() {
        // Tests are enabled by default.
    }

    protected boolean needsBaseline() {
        return false;
    }

    protected String getBasedir() {
        return "migration/concurrent";
    }

    protected abstract DataSource createDataSource() throws Exception;

    @Test
    public void migrateConcurrently() throws Exception {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    createFlyway().migrate();
                } catch (Exception e) {
                    LOG.error("Migrate failed", e);
                    error = e.getMessage();
                }
            }
        };

        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(runnable);
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i].start();
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i].join();
        }

        assertNull(error, error);
        final MigrationInfo[] applied = flyway.info().applied();
        int expected = 4;
        if (applied[0].getType() == MigrationType.SCHEMA) {
            expected++;
        }
        if (needsBaseline()) {
            expected++;
        }
        assertEquals(expected, applied.length);
        assertEquals("2.0", flyway.info().current().getVersion().toString());
        assertEquals(0, flyway.migrate());

        Connection connection = null;
        try {
            connection = concurrentMigrationDataSource.getConnection();
            assertEquals(2, new JdbcTemplate(connection, 0).queryForInt(
                    "SELECT COUNT(*) FROM " + getTableName()));
        } finally {
            JdbcUtils.closeConnection(connection);
        }
    }

    protected String getTableName() {
        return schemaQuoted + ".test_user";
    }

    private Flyway createFlyway() {
        Flyway newFlyway = new Flyway();
        newFlyway.setDataSource(concurrentMigrationDataSource);
        newFlyway.setLocations(getBasedir());
        newFlyway.setSchemas(schemaName);

        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("schema", schemaQuoted);

        newFlyway.setPlaceholders(placeholders);
        newFlyway.setMixed(isMixed());
        newFlyway.setBaselineVersionAsString("0.1");
        return newFlyway;
    }

    protected boolean isMixed() {
        return false;
    }
}
