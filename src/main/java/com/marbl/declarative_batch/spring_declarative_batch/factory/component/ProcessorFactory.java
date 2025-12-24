package com.marbl.declarative_batch.spring_declarative_batch.factory.component;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ComponentConfig;
import com.marbl.declarative_batch.spring_declarative_batch.exception.TypeNotSupportedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
public class ProcessorFactory {

    private final ApplicationContext context;

    private static final Map<String, Class<?>> PROCESSOR_TYPES = Map.of(
            "PassThroughItemProcessor", PassThroughItemProcessor.class,
            "ItemProcessor", ItemProcessor.class
    );

    public ProcessorFactory(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Create a processor of type <I, O> based on config or Spring context.
     */
    @SuppressWarnings("unchecked")
    public <I, O> ItemProcessor<I, O> createProcessor(ComponentConfig config) throws Exception {

        if (!StringUtils.hasText(config.getType())) {
            log.error("Processor type not provided in configuration");
            throw new IllegalArgumentException("Processor type must be provided");
        }

        ItemProcessor<I, O> processor = switch (config.getType()) {
            case "PassThroughItemProcessor" -> (ItemProcessor<I, O>) new PassThroughItemProcessor<I>();
            // case "CustomProcessor" -> (ItemProcessor<I, O>) new CustomProcessor<I, O>();
            default -> {
                log.error("Unknown processor type requested: {}", config.getType());
                throw new TypeNotSupportedException("Unknown processor type: " + config.getType());
            }
        };

        log.info("Created processor of type '{}'", config.getType());
        return processor;
    }

    /**
     * Check if the provided bean matches the expected processor type.
     */
    public boolean isAllowedProcessor(Object bean, String type) {
        Class<?> expected = PROCESSOR_TYPES.get(type);
        if (expected == null) {
            log.error("Unknown processor type for validation: {}", type);
            throw new TypeNotSupportedException("Unknown processor type: " + type);
        }
        boolean allowed = expected.isInstance(bean);
        log.debug("Processor bean '{}' is allowed for type '{}': {}", bean.getClass().getSimpleName(), type, allowed);
        return allowed;
    }
}
