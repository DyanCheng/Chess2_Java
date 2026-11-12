package com.chess2.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Keeps runtime connections off the session pool reserved for Flyway migrations. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.database.transaction-pooling", havingValue = "true")
public class ServerlessDatabaseConfig {
    private static final Pattern POOLER = Pattern.compile(
            "^(jdbc:postgresql://[a-zA-Z0-9.-]+\\.pooler\\.supabase\\.com):(5432|6543)/");

    @Bean
    static BeanPostProcessor serverlessRuntimePool() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String name) {
                if (bean instanceof HikariDataSource source) {
                    source.setJdbcUrl(transactionUrl(source.getJdbcUrl()));
                    source.setMaximumPoolSize(Math.min(source.getMaximumPoolSize(), 2));
                    source.setMinimumIdle(0);
                    source.setIdleTimeout(30000);
                    source.setMaxLifetime(120000);
                    LoggerFactory.getLogger(ServerlessDatabaseConfig.class).info(
                            "Supabase runtime uses transaction pooling; maximum connections per instance: {}", source.getMaximumPoolSize());
                }
                return bean;
            }
        };
    }

    static String transactionUrl(String url) {
        if (url == null || !POOLER.matcher(url).find()) {
            // Never include the JDBC URL in errors: it may contain credentials.
            throw new IllegalArgumentException("Transaction pooling requires a Supabase shared-pooler JDBC URL");
        }
        String runtime = POOLER.matcher(url).replaceFirst("$1:6543/");
        if (Pattern.compile("([?&])prepareThreshold=", Pattern.CASE_INSENSITIVE).matcher(runtime).find()) {
            return runtime.replaceAll("(?i)([?&])prepareThreshold=[^&]*", "$1prepareThreshold=0");
        }
        return runtime + (runtime.contains("?") ? "&" : "?") + "prepareThreshold=0";
    }
}
