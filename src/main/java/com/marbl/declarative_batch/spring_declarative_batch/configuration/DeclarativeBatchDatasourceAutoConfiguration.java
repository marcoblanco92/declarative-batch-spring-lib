package com.marbl.declarative_batch.spring_declarative_batch.configuration;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource.BatchDatasourceConfig;
import com.marbl.declarative_batch.spring_declarative_batch.factory.datasource.DataSourceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BatchDatasourceConfig.class)
public class DeclarativeBatchDatasourceAutoConfiguration {

    private final BatchDatasourceConfig batchDatasourceConfig;

    @Bean
    public Map<String, DataSource> dataSources() {
        Map<String, DataSource> map = new HashMap<>();
        DataSourceFactory dataSourceFactory = new DataSourceFactory();

        log.info("Initializing batch datasources...");
        batchDatasourceConfig.getDatasources().forEach((name, cfg) -> {
            DataSource ds = dataSourceFactory.create(name, cfg);
            map.put(name, ds);
            log.info("Registered datasource [{}] (type: {})", name, cfg.getType());
            log.debug("Datasource [{}] configuration details: {}", name, cfg);
        });

        log.info("Total datasources registered: {}", map.size());
        return map;
    }

    @Bean
    @Primary
    public DataSource mainDataSource(Map<String, DataSource> dataSources) {
        return batchDatasourceConfig.getDatasources().entrySet().stream()
                .filter(e -> e.getValue().isMain())
                .findFirst()
                .map(e -> {
                    log.info("Using [{}] as main datasource", e.getKey());
                    return dataSources.get(e.getKey());
                })
                .orElseThrow(() -> new IllegalStateException(
                        "[DeclarativeBatchDatasourceAutoConfiguration] No main datasource found. " +
                                "Please define one with 'main: true'."
                ));
    }
}
