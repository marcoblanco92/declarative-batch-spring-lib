package com.marbl.declarative_batch.spring_declarative_batch.configuration;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.BatchJobConfig;
import com.marbl.declarative_batch.spring_declarative_batch.factory.component.*;
import com.marbl.declarative_batch.spring_declarative_batch.factory.job.BatchJobFactory;
import com.marbl.declarative_batch.spring_declarative_batch.factory.step.StepFactory;
import com.marbl.declarative_batch.spring_declarative_batch.support.executor.BatchJobExecutor;
import com.marbl.declarative_batch.spring_declarative_batch.support.incrementer.DatabaseRunIdIncrementer;
import com.marbl.declarative_batch.spring_declarative_batch.support.log.LoggingStepListener;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(JobRepository.class)
@EnableConfigurationProperties(BatchJobConfig.class)
@AutoConfigureAfter(DeclarativeBatchInfrastructureAutoConfiguration.class)
public class DeclarativeBatchJobAutoConfiguration {

    @Bean
    @ConditionalOnBean({JobLauncher.class, JobRegistry.class})
    public BatchJobExecutor batchJobExecutor(BatchJobConfig jobConfig,
                                             BatchJobFactory batchJobFactory,
                                             JobLauncher jobLauncher,
                                             JobRegistry jobRegistry) {
        return new BatchJobExecutor(jobConfig, batchJobFactory, jobLauncher, jobRegistry);
    }

    @Bean
    @ConditionalOnBean({JobLauncher.class, JobRegistry.class, JobRepository.class, PlatformTransactionManager.class})
    public BatchJobFactory batchJobFactory(BatchJobConfig jobConfig,
                                           JobRepository jobRepository,
                                           JobExplorer jobExplorer,
                                           ApplicationContext context,
                                           @Nullable RunIdIncrementer runIdIncrementer,
                                           PlatformTransactionManager transactionManager) {
        return new BatchJobFactory(jobConfig, jobRepository, jobExplorer, context, runIdIncrementer, transactionManager);
    }

    @Bean
    @ConditionalOnBean({JobRepository.class, PlatformTransactionManager.class})
    public StepFactory stepFactory(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   LoggingStepListener loggingStepListener,
                                   ReaderFactory readerFactory,
                                   ProcessorFactory processorFactory,
                                   WriterFactory writerFactory,
                                   ListenerFactory listenerFactory) {
        return new StepFactory(jobRepository, transactionManager, loggingStepListener, readerFactory, processorFactory, writerFactory, listenerFactory);
    }

    @Bean
    @Primary
    public ReaderFactory readerFactory(ApplicationContext context) { return new ReaderFactory(context); }

    @Bean
    @Primary
    public ProcessorFactory processorFactory(ApplicationContext context) { return new ProcessorFactory(context); }

    @Bean
    @Primary
    public WriterFactory writerFactory(ApplicationContext context) { return new WriterFactory(context); }

    @Bean
    @Primary
    public ListenerFactory listenerFactory(ApplicationContext context) { return new ListenerFactory(context); }

    @Bean
    @Primary
    public LoggingStepListener loggingStepListener() { return new LoggingStepListener(); }

    @Bean
    @Primary
    @ConditionalOnBean(JobExplorer.class)
    public DatabaseRunIdIncrementer databaseRunIdIncrementer(BatchJobConfig batchJobConfig, JobExplorer jobExplorer) {
        return new DatabaseRunIdIncrementer(batchJobConfig, jobExplorer);
    }

    @Bean
    @Profile("local")
    public RunIdIncrementer runIdIncrementer() {
        log.info("RunIdIncrementer bean created (profile: local)");
        return new RunIdIncrementer();
    }
}
