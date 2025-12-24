package com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchSchemaInitializer {

    private final DataSource dataSource;
    private final BatchDatasourceConfig batchDatasourceConfig;

    @Bean
    public DataSourceInitializer batchDataSourceInitializer() {
        BatchProperties.Jdbc jdbc = batchDatasourceConfig.getBatchProperties().getJdbc();

        log.info("[BatchSchemaInitializer] Checking schema initialization mode for platform '{}'", jdbc.getPlatform());

        // Initialize only if explicitly requested
        if (jdbc.getInitializeSchema() != DatabaseInitializationMode.ALWAYS) {
            log.info("[BatchSchemaInitializer] Batch schema initialization skipped (mode: {}).",
                    jdbc.getInitializeSchema());
            return null;
        }

        log.info("[BatchSchemaInitializer] Schema initialization requested (mode: ALWAYS).");
        log.debug("[BatchSchemaInitializer] BatchProperties.Jdbc configuration: {}", jdbc);

        // Determine the correct script based on the platform
        String schemaLocation = getSchemaPathForPlatform(jdbc.getPlatform());
        log.info("[BatchSchemaInitializer] Using schema script: {}", schemaLocation);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(schemaLocation));

        // Configure the initializer
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);

        log.info("[BatchSchemaInitializer] DataSourceInitializer configured successfully.");
        return initializer;
    }

    /**
     * Returns the SQL script path to use for the given platform.
     * Points directly to the script included in spring-batch-core.
     */
    private String getSchemaPathForPlatform(String platform) {
        log.debug("[BatchSchemaInitializer] Resolving schema script for platform '{}'", platform);

        if ("postgres".equalsIgnoreCase(platform) || "postgresql".equalsIgnoreCase(platform)) {
            return "org/springframework/batch/core/schema-postgresql.sql";
        } else if ("mysql".equalsIgnoreCase(platform)) {
            return "org/springframework/batch/core/schema-mysql.sql";
        } else if ("h2".equalsIgnoreCase(platform)) {
            return "org/springframework/batch/core/schema-h2.sql";
        } else {
            String message = String.format(
                    "[BatchSchemaInitializer] Unsupported database platform: '%s'. " +
                            "Please configure a valid 'spring.batch.jdbc.platform' (e.g. 'postgres', 'mysql', 'h2').",
                    platform
            );
            log.error(message);
            throw new IllegalArgumentException(message);
        }
    }
}
