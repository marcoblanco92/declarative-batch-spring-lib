package com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@Data
@Validated
@ConfigurationProperties(prefix = "bulk")
public class BatchDatasourceConfig {
    @Valid
    private Map<String, DataSourceConfig> datasources = new HashMap<>();

    @NestedConfigurationProperty
    private BatchProperties batchProperties = new BatchProperties();

    @AssertTrue(message = "Only one datasource can be flagged as 'Main'")
    public boolean isOnlyOneMainDatasource() {
        if (datasources == null || datasources.isEmpty()) {
            return true;
        }
        long count = datasources.values().stream()
                .filter(ds -> Boolean.TRUE.equals(ds.isMain()))
                .count();
        return count <= 1;
    }
}
