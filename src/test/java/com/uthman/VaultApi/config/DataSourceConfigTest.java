package com.uthman.VaultApi.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceConfigTest {

    @Test
    void convertsPostgresUrlToJdbc() {
        assertEquals("jdbc:postgresql://host:5432/db",
                DataSourceConfig.toJdbcUrl("postgres://host:5432/db"));
        assertEquals("jdbc:postgresql://host:5432/db",
                DataSourceConfig.toJdbcUrl("postgresql://host:5432/db"));
    }

    @Test
    void leavesJdbcUrlUntouched() {
        String url = "jdbc:postgresql://host:5432/db";
        assertEquals(url, DataSourceConfig.toJdbcUrl(url));
    }

    @Test
    void keepsQueryParamsWhenConverting() {
        assertEquals("jdbc:postgresql://host:5432/db?sslmode=require",
                DataSourceConfig.toJdbcUrl("postgresql://host:5432/db?sslmode=require"));
    }

    @Test
    void parsesEmbeddedCredentials() {
        String[] creds = DataSourceConfig.parseCredentials(
                "postgresql://goalforge:secret@dpg-abc-a/goalforge");
        assertNotNull(creds);
        assertEquals("goalforge", creds[0]);
        assertEquals("secret", creds[1]);
    }

    @Test
    void returnsNullWhenUrlHasNoCredentials() {
        assertNull(DataSourceConfig.parseCredentials("postgresql://host:5432/db"));
    }

    @Test
    void detectsPostgresDriver() {
        assertEquals("org.postgresql.Driver",
                DataSourceConfig.driverFor("jdbc:postgresql://host:5432/db"));
    }

    @Test
    void detectsH2Driver() {
        assertEquals("org.h2.Driver",
                DataSourceConfig.driverFor("jdbc:h2:mem:testdb"));
    }
}
