package com.marbl.declarative_batch.spring_declarative_batch.configuration;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource.BatchDatasourceConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource.DataSourceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(name = "mainDataSource")
@EnableConfigurationProperties(BatchDatasourceConfig.class)
public class DeclarativeBatchInfrastructureAutoConfiguration extends DefaultBatchConfiguration {

    private final BatchDatasourceConfig batchDatasourceConfig;

    @Bean
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager(@Qualifier("mainDataSource") DataSource mainDataSource) {
        log.info("Creating transaction manager for main datasource");
        return new DataSourceTransactionManager(mainDataSource);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public JobRepository jobRepository(@Qualifier("mainDataSource") DataSource mainDataSource,
                                       PlatformTransactionManager transactionManager) throws Exception {

        log.info("Initializing JobRepository...");
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(mainDataSource);
        factory.setTransactionManager(transactionManager);
        factory.setTablePrefix(batchDatasourceConfig.getBatchProperties().getJdbc().getTablePrefix());

        String isolationLevel = batchDatasourceConfig.getDatasources().values().stream()
                .filter(DataSourceConfig::isMain)
                .map(cfg -> cfg.getIsolationLevelEnum().name())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "[JobRepository] Unable to determine isolation level for main datasource."
                ));

        String databaseType = batchDatasourceConfig.getDatasources().values().stream()
                .filter(DataSourceConfig::isMain)
                .map(DataSourceConfig::getType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "[JobRepository] Unable to determine database type for main datasource."
                ));

        factory.setIsolationLevelForCreate(isolationLevel);
        factory.setDatabaseType(databaseType);
        factory.afterPropertiesSet();

        log.info("JobRepository initialized successfully");
        return factory.getObject();
    }

    @Bean
    @ConditionalOnMissingBean
    public JobExplorer jobExplorer(@Qualifier("mainDataSource") DataSource mainDataSource,
                                   PlatformTransactionManager transactionManager) throws Exception {

        log.info("Initializing JobExplorer...");
        JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
        factory.setDataSource(mainDataSource);
        factory.setTransactionManager(transactionManager);
        factory.setTablePrefix(batchDatasourceConfig.getBatchProperties().getJdbc().getTablePrefix());
        factory.afterPropertiesSet();

        log.info("JobExplorer initialized successfully");
        return factory.getObject();
    }
}
