package com.marbl.declarative_batch.spring_declarative_batch.configuration;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource.BatchDatasourceConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource.DataSourceConfig;
import com.marbl.declarative_batch.spring_declarative_batch.factory.datasource.DataSourceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchCoreConfig extends DefaultBatchConfiguration {

    private final DataSourceFactory dataSourceFactory;
    private final BatchDatasourceConfig batchDatasourceConfig;

    @Bean
    public Map<String, DataSource> dataSources() {
        Map<String, DataSource> map = new HashMap<>();

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
                        "[BatchCoreConfig] No main datasource found. " +
                                "Please ensure that one datasource is defined with 'main: true' " +
                                "under the 'bulk.datasources' configuration section."
                ));
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource mainDataSource) {
        log.info("Creating transaction manager for main datasource");
        return new DataSourceTransactionManager(mainDataSource);
    }

    @Bean
    public JobRepository jobRepository(DataSource mainDataSource,
                                       PlatformTransactionManager transactionManager) throws Exception {

        log.info("Initializing JobRepository...");

        JobRepositoryFactoryBean jobRepositoryFactory = new JobRepositoryFactoryBean();
        jobRepositoryFactory.setDataSource(mainDataSource);
        jobRepositoryFactory.setTransactionManager(transactionManager);
        jobRepositoryFactory.setTablePrefix(batchDatasourceConfig.getBatchProperties().getJdbc().getTablePrefix());

        String isolationLevel = batchDatasourceConfig.getDatasources().values().stream()
                .filter(DataSourceConfig::isMain)
                .map(dataSourceConfig -> dataSourceConfig.getIsolationLevelEnum().name())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "[BatchCoreConfig] Unable to determine isolation level for JobRepository. " +
                                "Ensure that the main datasource defines a valid 'isolationLevel' property."
                ));

        String databaseType = batchDatasourceConfig.getDatasources().values().stream()
                .filter(DataSourceConfig::isMain)
                .map(DataSourceConfig::getType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "[BatchCoreConfig] Unable to determine database type for JobRepository. " +
                                "Ensure that the main datasource defines a valid 'type' property."
                ));

        log.debug("JobRepository configuration: isolationLevel={}, databaseType={}, tablePrefix={}",
                isolationLevel, databaseType, batchDatasourceConfig.getBatchProperties().getJdbc().getTablePrefix());

        jobRepositoryFactory.setIsolationLevelForCreate(isolationLevel);
        jobRepositoryFactory.setDatabaseType(databaseType);
        jobRepositoryFactory.afterPropertiesSet();

        log.info("JobRepository initialized successfully");
        return jobRepositoryFactory.getObject();
    }

    @Bean
    public JobExplorer jobExplorer(DataSource mainDataSource,
                                   BatchDatasourceConfig batchDatasourceConfig) throws Exception {

        log.info("Initializing JobExplorer...");

        JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
        jobExplorerFactoryBean.setDataSource(mainDataSource);
        jobExplorerFactoryBean.setTransactionManager(transactionManager(mainDataSource));
        jobExplorerFactoryBean.setTablePrefix(batchDatasourceConfig.getBatchProperties().getJdbc().getTablePrefix());
        jobExplorerFactoryBean.afterPropertiesSet();

        log.debug("JobExplorer configuration: tablePrefix={}",
                batchDatasourceConfig.getBatchProperties().getJdbc().getTablePrefix());
        log.info("JobExplorer initialized successfully");

        return jobExplorerFactoryBean.getObject();
    }

    @Bean
    @Profile("local")
    public RunIdIncrementer runIdIncrementer() {
        log.info("RunIdIncrementer bean created (profile: local)");
        return new RunIdIncrementer();
    }
}
