package com.chess2.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.junit.jupiter.api.Assertions.*;

class ServerlessDatabaseConfigTest {
    private static final String URL = "jdbc:postgresql://aws-0-eu-west-1.pooler.supabase.com:5432/postgres?user=postgres.test&password=fake%26encoded&sslmode=require";

    @Configuration(proxyBeanMethods=false)
    @EnableConfigurationProperties
    static class PoolFixture {
        @Bean @ConfigurationProperties("spring.datasource.hikari")
        HikariDataSource dataSource() { return new HikariDataSource(); }
    }

    @Test void appliesAfterPropertyBindingAndPreservesEncodedCredentials() {
        new ApplicationContextRunner().withUserConfiguration(PoolFixture.class, ServerlessDatabaseConfig.class)
                .withPropertyValues("app.database.transaction-pooling=true",
                        "spring.datasource.hikari.jdbc-url="+URL, "spring.datasource.hikari.maximum-pool-size=4")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    HikariDataSource pool = context.getBean(HikariDataSource.class);
                    assertEquals(URL.replace(":5432/", ":6543/")+"&prepareThreshold=0",pool.getJdbcUrl());
                    assertEquals(2,pool.getMaximumPoolSize());
                    assertEquals(0,pool.getMinimumIdle());
                });
    }
    @Test void disablesExistingServerPreparedStatements() {
        assertEquals(URL.replace(":5432/", ":6543/")+"&prepareThreshold=0",
                ServerlessDatabaseConfig.transactionUrl(URL+"&prepareThreshold=5"));
    }
    @Test void leavesLocalDatasourceUntouchedWhenNotEnabled() {
        new ApplicationContextRunner().withUserConfiguration(PoolFixture.class, ServerlessDatabaseConfig.class)
                .withPropertyValues("spring.datasource.hikari.jdbc-url=jdbc:h2:mem:local")
                .run(context -> assertEquals("jdbc:h2:mem:local",context.getBean(HikariDataSource.class).getJdbcUrl()));
    }
    @Test void rejectsUnknownHostWithoutDisclosingUrl() {
        var error=assertThrows(IllegalArgumentException.class,()->ServerlessDatabaseConfig.transactionUrl("jdbc:postgresql://example.com:5432/db?password=secret"));
        assertFalse(error.getMessage().contains("secret"));
    }
}
