package com.marbl.declarative_batch.spring_declarative_batch.factory.step;

import com.marbl.declarative_batch.spring_declarative_batch.annotation.BulkBatchProcessor;
import com.marbl.declarative_batch.spring_declarative_batch.annotation.BulkBatchReader;
import com.marbl.declarative_batch.spring_declarative_batch.annotation.BulkBatchWriter;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ListenerConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.StepsConfig;
import com.marbl.declarative_batch.spring_declarative_batch.exception.InvalidBeanException;
import com.marbl.declarative_batch.spring_declarative_batch.factory.component.ListenerFactory;
import com.marbl.declarative_batch.spring_declarative_batch.factory.component.ProcessorFactory;
import com.marbl.declarative_batch.spring_declarative_batch.factory.component.ReaderFactory;
import com.marbl.declarative_batch.spring_declarative_batch.factory.component.WriterFactory;
import com.marbl.declarative_batch.spring_declarative_batch.support.log.LoggingStepListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@RequiredArgsConstructor
public class StepFactory {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final LoggingStepListener loggingStepListener;

    private final ReaderFactory readerFactory;
    private final ProcessorFactory processorFactory;
    private final WriterFactory writerFactory;
    private final ListenerFactory listenerFactory;

    /**
     * Build a typed Step from a YAML config or AbstractSteplet.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <I, O> Step createStep(StepsConfig config,
                                  ItemReader<I> reader,
                                  ItemProcessor<I, O> processor,
                                  ItemWriter<O> writer) throws Exception {

        log.info("Creating step '{}'", config.getName());

        // --- Validate components passed from Steplet ---
        validateReader(reader, config);
        validateProcessor(processor, config);
        validateWriter(writer, config);

        // --- Build or reuse typed components ---
        ItemReader<I> finalReader = reader != null
                ? reader
                : readerFactory.createReader(config.getReader(), config.getChunk());

        ItemProcessor<I, O> finalProcessor = processor != null
                ? processor
                : processorFactory.createProcessor(config.getProcessor());

        ItemWriter<O> finalWriter = writer != null
                ? writer
                : writerFactory.createWriter(config.getWriter());

        // --- Build chunk step ---
        StepBuilder stepBuilder = new StepBuilder(config.getName(), jobRepository);
        SimpleStepBuilder<I, O> chunkStep = stepBuilder
                .<I, O>chunk(config.getChunk(), transactionManager)
                .reader(finalReader)
                .processor(finalProcessor)
                .writer(finalWriter);

        // --- Attach common logging listener ---
        chunkStep.listener((StepExecutionListener) loggingStepListener);
        chunkStep.listener((ChunkListener) loggingStepListener);

        // --- Attach additional listeners from YAML config ---
        attachStepListeners(chunkStep, config);

        // --- Configure fault tolerance if defined ---
        if (config.getRetry() != null || config.getSkip() != null || config.getTransaction() != null) {
            chunkStep = configureFaultTolerance(chunkStep, config);
        }

        Step step = chunkStep.build();
        log.info("Step '{}' created successfully", config.getName());
        return step;
    }

    // -------------------------
    // Helper methods
    // -------------------------

    private <I> void validateReader(ItemReader<I> reader, StepsConfig config) {
        if (reader == null) return;
        BulkBatchReader ann = reader.getClass().getAnnotation(BulkBatchReader.class);
        if (ann == null || !ann.name().equals(config.getReader().getName())) {
            throw new InvalidBeanException(
                    "Reader passed from Steplet does not match YAML: expected name " + config.getReader().getName()
            );
        }
        if (!readerFactory.isAllowedReader(reader, config.getReader().getType())) {
            throw new InvalidBeanException(
                    "Reader passed from Steplet does not match YAML type: expected " + config.getReader().getType()
            );
        }
        log.debug("Validated reader '{}' for step '{}'", config.getReader().getName(), config.getName());
    }

    private <I, O> void validateProcessor(ItemProcessor<I, O> processor, StepsConfig config) {
        if (processor == null) return;
        BulkBatchProcessor ann = processor.getClass().getAnnotation(BulkBatchProcessor.class);
        if (ann == null || !ann.name().equals(config.getProcessor().getName())) {
            throw new InvalidBeanException(
                    "Processor passed from Steplet does not match YAML: expected name " + config.getProcessor().getName()
            );
        }
        if (!processorFactory.isAllowedProcessor(processor, config.getProcessor().getType())) {
            throw new InvalidBeanException(
                    "Processor passed from Steplet does not match YAML type: expected " + config.getProcessor().getType()
            );
        }
        log.debug("Validated processor '{}' for step '{}'", config.getProcessor().getName(), config.getName());
    }

    private <O> void validateWriter(ItemWriter<O> writer, StepsConfig config) {
        if (writer == null) return;
        BulkBatchWriter ann = writer.getClass().getAnnotation(BulkBatchWriter.class);
        if (ann == null || !ann.name().equals(config.getWriter().getName())) {
            throw new InvalidBeanException(
                    "Writer passed from Steplet does not match YAML: expected name " + config.getWriter().getName()
            );
        }
        if (!writerFactory.isAllowedWriter(writer, config.getWriter().getType())) {
            throw new InvalidBeanException(
                    "Writer passed from Steplet does not match YAML type: expected " + config.getWriter().getType()
            );
        }
        log.debug("Validated writer '{}' for step '{}'", config.getWriter().getName(), config.getName());
    }

    private <I, O> void attachStepListeners(SimpleStepBuilder<I, O> chunkStep, StepsConfig config) {
        if (config.getListeners() == null) return;

        for (ListenerConfig listenerConfig : config.getListeners()) {
            try {
                Object listener = listenerFactory.createStepListener(listenerConfig);
                if (listener == null) continue;

                if (listener instanceof StepExecutionListener sel) {
                    chunkStep.listener(sel);
                    log.info("Attached StepExecutionListener '{}' to step '{}'",
                            listenerConfig.getName(), config.getName());
                } else if (listener instanceof ItemReadListener irl) {
                    chunkStep.listener(irl);
                    log.info("Attached ItemReadListener '{}' to step '{}'",
                            listenerConfig.getName(), config.getName());
                } else if (listener instanceof ItemWriteListener iwl) {
                    chunkStep.listener(iwl);
                    log.info("Attached ItemWriteListener '{}' to step '{}'",
                            listenerConfig.getName(), config.getName());
                } else if (listener instanceof ItemProcessListener ipl) {
                    chunkStep.listener(ipl);
                    log.info("Attached ItemProcessListener '{}' to step '{}'",
                            listenerConfig.getName(), config.getName());
                } else {
                    log.warn("Listener '{}' of type '{}' is not handled",
                            listenerConfig.getName(), listenerConfig.getType());
                }
            } catch (Exception e) {
                log.error("Error attaching listener '{}' to step '{}': {}", listenerConfig.getName(), config.getName(), e.getMessage(), e);
            }
        }
    }

    private <I, O> SimpleStepBuilder<I, O> configureFaultTolerance(SimpleStepBuilder<I, O> chunkStep, StepsConfig config) {
        FaultTolerantStepBuilder<I, O> faultStep = chunkStep.faultTolerant();

        // Retry
        if (config.getRetry() != null) {
            faultStep.retryLimit(config.getRetry().getLimit());
            if (config.getRetry().getExceptions() != null) {
                for (Class<? extends Throwable> ex : config.getRetry().getExceptions()) {
                    faultStep.retry(ex);
                }
            }
            log.info("Configured retry for step '{}': limit={}, exceptions={}",
                    config.getName(), config.getRetry().getLimit(), config.getRetry().getExceptions());
        }

        // Skip
        if (config.getSkip() != null) {
            faultStep.skipLimit(config.getSkip().getLimit());
            if (config.getSkip().getExceptionsToSkip() != null) {
                for (Class<? extends Throwable> ex : config.getSkip().getExceptionsToSkip()) {
                    faultStep.skip(ex);
                }
            }
            if (config.getSkip().getExceptionsNoSkip() != null) {
                for (Class<? extends Throwable> ex : config.getSkip().getExceptionsNoSkip()) {
                    faultStep.noSkip(ex);
                }
            }
            log.info("Configured skip for step '{}': limit={}, exceptions={}, noSkipExceptions={}",
                    config.getName(),
                    config.getSkip().getLimit(),
                    config.getSkip().getExceptionsToSkip(),
                    config.getSkip().getExceptionsNoSkip());
        }

        // Transaction rollback
        if (config.getTransaction() != null) {
            if (config.getTransaction().getNoRollbackExceptions() != null) {
                for (Class<? extends Throwable> ex : config.getTransaction().getNoRollbackExceptions()) {
                    faultStep.noRollback(ex);
                }
            }
            if (config.getTransaction().isReaderInTransaction()) {
                faultStep.readerIsTransactionalQueue();
            }
            log.info("Configured transaction for step '{}': noRollbackExceptions={}, isReaderInTransactionQueue={}",
                    config.getName(),
                    config.getTransaction().getNoRollbackExceptions(),
                    config.getTransaction().isReaderInTransaction());
        }

        return faultStep;
    }
}
