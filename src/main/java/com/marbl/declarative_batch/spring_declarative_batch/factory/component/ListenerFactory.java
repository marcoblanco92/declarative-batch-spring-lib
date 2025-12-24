package com.marbl.declarative_batch.spring_declarative_batch.factory.component;

import com.marbl.declarative_batch.spring_declarative_batch.annotation.*;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ListenerConfig;
import com.marbl.declarative_batch.spring_declarative_batch.exception.BatchException;
import com.marbl.declarative_batch.spring_declarative_batch.exception.InvalidBeanException;
import com.marbl.declarative_batch.spring_declarative_batch.exception.TypeNotSupportedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Map;

import static com.marbl.declarative_batch.spring_declarative_batch.utils.ListenerUtils.resolveStepListenerClass;

@Slf4j
public class ListenerFactory {

    private final ApplicationContext context;

    public ListenerFactory(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Creates a JobExecutionListener from configuration or Spring context.
     */
    public JobExecutionListener createJobListener(ListenerConfig config) {
        if (!StringUtils.hasText(config.getType())) {
            log.error("Job listener creation failed: 'type' field is empty in ListenerConfig");
            throw new TypeNotSupportedException("Listener type must be provided");
        }

        if (!StringUtils.hasText(config.getName())) {
            log.debug("No explicit job listener bean defined in config '{}'", config);
            return null; // no explicit bean
        }

        try {
            // Find all beans annotated with @BulkBatchListener
            Map<String, Object> beans = context.getBeansWithAnnotation(BulkBatchListener.class);

            Object targetBean = beans.values().stream()
                    .filter(bean -> {
                        BulkBatchListener ann = bean.getClass().getAnnotation(BulkBatchListener.class);
                        return ann != null && ann.name().equals(config.getName());
                    })
                    .findFirst()
                    .orElseThrow(() -> new InvalidBeanException(
                            "No Job listener bean found with annotation name: " + config.getName()
                    ));

            if (!(targetBean instanceof JobExecutionListener listener)) {
                log.error("Bean '{}' annotated with @BulkBatchJobListener does not implement JobExecutionListener", config.getName());
                throw new InvalidBeanException(
                        "Bean '" + config.getName() + "' annotated with @BulkBatchJobListener does not implement JobExecutionListener"
                );
            }

            log.info("JobExecutionListener '{}' successfully found for type '{}'", config.getName(), config.getType());
            log.debug("Using JobExecutionListener bean '{}' for config: {}", config.getName(), config);
            return listener;

        } catch (Exception e) {
            log.error("Error creating JobExecutionListener '{}': {}", config.getName(), e.getMessage(), e);
            throw new BatchException(e.getMessage(), e);
        }
    }

    /**
     * Creates a StepListener from configuration or Spring context.
     * Supports StepExecutionListener, ItemReadListener<I>, ItemProcessListener<I,O>, ItemWriteListener<O>.
     */
    public StepListener createStepListener(ListenerConfig config) {
        if (!StringUtils.hasText(config.getType())) {
            log.error("Step listener creation failed: 'type' field is empty in ListenerConfig");
            throw new TypeNotSupportedException("Listener type must be provided");
        }

        if (!StringUtils.hasText(config.getName())) {
            log.debug("No explicit step listener bean defined in config '{}'", config);
            return null;
        }

        Class<?> expectedType = resolveStepListenerClass(config.getType());
        Object targetBean = null;

        try {
            // 1️⃣ Search by annotations
            targetBean = findBeanByAnnotation(config.getName(), BulkBatchListener.class, expectedType);
            if (targetBean == null)
                targetBean = findBeanByAnnotation(config.getName(), BulkBatchSteplet.class, expectedType);
            if (targetBean == null)
                targetBean = findBeanByAnnotation(config.getName(), BulkBatchReader.class, expectedType);
            if (targetBean == null)
                targetBean = findBeanByAnnotation(config.getName(), BulkBatchProcessor.class, expectedType);
            if (targetBean == null)
                targetBean = findBeanByAnnotation(config.getName(), BulkBatchWriter.class, expectedType);

            if (targetBean == null) {
                log.error("No Step listener bean found for name '{}'", config.getName());
                throw new InvalidBeanException("No bean found implementing listener '" + config.getName() + "'");
            }

            if (!expectedType.isInstance(targetBean)) {
                log.error("Bean '{}' does not implement expected StepListener type '{}'", config.getName(), config.getType());
                throw new InvalidBeanException(
                        "Bean '" + config.getName() + "' does not implement expected type: " + config.getType()
                );
            }

            log.info("StepListener '{}' successfully found for type '{}'", config.getName(), config.getType());
            log.debug("Using StepListener bean '{}' for config: {}", config.getName(), config);
            return (StepListener) targetBean;

        } catch (Exception e) {
            log.error("Error creating StepListener '{}': {}", config.getName(), e.getMessage(), e);
            throw new BatchException(e.getMessage(), e);
        }
    }

    private Object findBeanByAnnotation(String name, Class<? extends Annotation> annotationType, Class<?> expectedType) {
        Map<String, Object> beans = context.getBeansWithAnnotation(annotationType);

        return beans.values().stream()
                .filter(bean -> {
                    Annotation ann = bean.getClass().getAnnotation(annotationType);
                    try {
                        // All BulkBatch* annotations have a `name()` method
                        String annName = (String) annotationType.getMethod("name").invoke(ann);
                        return annName.equals(name) && expectedType.isInstance(bean);
                    } catch (Exception e) {
                        log.debug("Failed to read annotation name from bean '{}': {}", bean.getClass().getSimpleName(), e.getMessage());
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
    }
}
