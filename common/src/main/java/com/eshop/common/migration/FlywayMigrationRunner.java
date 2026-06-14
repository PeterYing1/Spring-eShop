package com.eshop.common.migration;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.env.Environment;

import java.io.IOException;

@AutoConfiguration
public class FlywayMigrationRunner implements ApplicationRunner {
    private final Environment environment;
    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public FlywayMigrationRunner(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (isDisabled()) {
            return;
        }

        String locations = environment.getProperty("spring.flyway.locations", "classpath:db/migration");
        if (!hasMigrationScripts(locations)) {
            return;
        }

        String url = optional("eshop.migrations.url", "spring.datasource.url");
        if (url == null || url.isBlank()) {
            return;
        }

        String user = environment.getProperty("eshop.migrations.user",
                environment.getProperty("spring.datasource.username", "sa"));
        String password = environment.getProperty("eshop.migrations.password",
                environment.getProperty("spring.datasource.password", ""));

        Flyway.configure()
                .dataSource(url, user, password)
                .locations(locations)
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    private boolean isDisabled() {
        return "false".equalsIgnoreCase(environment.getProperty("eshop.migrations.enabled", "auto"));
    }

    private boolean hasMigrationScripts(String locations) throws IOException {
        for (String location : locations.split(",")) {
            String normalized = location.trim();
            if (normalized.startsWith("classpath:")) {
                normalized = "classpath*:" + normalized.substring("classpath:".length());
            }
            Resource[] resources = resourceResolver.getResources(normalized + "/*");
            if (resources.length > 0) {
                return true;
            }
        }
        return false;
    }

    private String optional(String primaryKey, String fallbackKey) {
        String value = environment.getProperty(primaryKey);
        if (value == null || value.isBlank()) {
            value = environment.getProperty(fallbackKey);
        }
        return value;
    }
}
