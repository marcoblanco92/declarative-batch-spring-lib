package com.marbl.declarative_batch.spring_declarative_batch.configuration;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.BatchJobConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource.BatchDatasourceConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.datasource.BatchSchemaInitializer;
import com.marbl.declarative_batch.spring_declarative_batch.factory.component.ListenerFactory;
import com.marbl.declarative_batch.spring_declarative_batch.factory.component.ProcessorFactory;
import com.marbl.declarative_batch.spring_declarative_batch.factory.component.ReaderFactory;
import com.marbl.declarative_batch.spring_declarative_batch.factory.component.WriterFactory;
import com.marbl.declarative_batch.spring_declarative_batch.factory.datasource.DataSourceFactory;
import com.marbl.declarative_batch.spring_declarative_batch.factory.job.BatchJobFactory;
import com.marbl.declarative_batch.spring_declarative_batch.factory.step.StepFactory;
import com.marbl.declarative_batch.spring_declarative_batch.support.executor.BatchJobExecutor;
import com.marbl.declarative_batch.spring_declarative_batch.support.log.LoggingStepListener;
import jakarta.annotation.Nullable;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration
@ConditionalOnClass(DefaultBatchConfiguration.class)
@EnableConfigurationProperties({
        BatchDatasourceConfig.class,
        BatchJobConfig.class
})
@Import({
        BatchCoreConfig.class,
        BatchSchemaInitializer.class
})
public class DeclarativeBatchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataSourceFactory dataSourceFactory() {
        return new DataSourceFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public BatchJobExecutor batchJobExecutor(
            BatchJobConfig jobConfig,
            BatchJobFactory batchJobFactory,
            JobLauncher jobLauncher,
            JobRegistry jobRegistry
    ) {
        return new BatchJobExecutor(
                jobConfig,
                batchJobFactory,
                jobLauncher,
                jobRegistry
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public BatchJobFactory batchJobFactory(
            BatchJobConfig jobConfig,
            JobRepository jobRepository,
            JobExplorer jobExplorer,
            ApplicationContext context,
            @Nullable RunIdIncrementer runIdIncrementer,
            PlatformTransactionManager transactionManager
    ) {
        return new BatchJobFactory(
                jobConfig,
                jobRepository,
                jobExplorer,
                context,
                runIdIncrementer,
                transactionManager
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public StepFactory stepFactory(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            LoggingStepListener loggingStepListener,
            ReaderFactory readerFactory,
            ProcessorFactory processorFactory,
            WriterFactory writerFactory,
            ListenerFactory listenerFactory
    ) {
        return new StepFactory(
                jobRepository,
                transactionManager,
                loggingStepListener,
                readerFactory,
                processorFactory,
                writerFactory,
                listenerFactory
        );
    }

    @Bean
    public ReaderFactory readerFactory(ApplicationContext context) {
        return new ReaderFactory(context);
    }

    @Bean
    public ProcessorFactory processorFactory(ApplicationContext context) {
        return new ProcessorFactory(context);
    }

    @Bean
    public WriterFactory writerFactory(ApplicationContext context) {
        return new WriterFactory(context);
    }

    @Bean
    public ListenerFactory listenerFactory(ApplicationContext context) {
        return new ListenerFactory(context);
    }


}
