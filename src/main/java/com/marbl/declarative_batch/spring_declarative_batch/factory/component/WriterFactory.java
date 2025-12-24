package com.marbl.declarative_batch.spring_declarative_batch.factory.component;

import com.marbl.declarative_batch.spring_declarative_batch.builder.writer.FlatFileWriterBuilder;
import com.marbl.declarative_batch.spring_declarative_batch.builder.writer.JdbcBatchWriterBuilder;
import com.marbl.declarative_batch.spring_declarative_batch.exception.TypeNotSupportedException;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ComponentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
public class WriterFactory {

    private final ApplicationContext context;

    private static final Map<String, Class<?>> WRITER_TYPES = Map.of(
            "FlatFileItemWriter", org.springframework.batch.item.file.FlatFileItemWriter.class,
            "JdbcBatchItemWriter", org.springframework.batch.item.database.JdbcBatchItemWriter.class,
            "KafkaItemWriter", org.springframework.batch.item.kafka.KafkaItemWriter.class,
            "ItemWriter", ItemWriter.class
    );

    public WriterFactory(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Creates a typed ItemWriter<O> based on configuration or Spring context.
     */
    @SuppressWarnings("unchecked")
    public <O> ItemWriter<O> createWriter(ComponentConfig config) throws Exception {
        if (!StringUtils.hasText(config.getType())) {
            log.error("Writer creation failed: 'type' field is empty in ComponentConfig");
            throw new IllegalArgumentException("Writer type must be provided");
        }

        String writerType = config.getType();
        log.debug("Starting writer creation: type='{}' for component '{}'", writerType, config.getName());

        ItemWriter<O> writer;
        try {
            writer = switch (writerType) {
                case "FlatFileItemWriter" -> {
                    log.debug("Using FlatFileWriterBuilder for configuration '{}'", config.getName());
                    yield FlatFileWriterBuilder.build(config);
                }
                case "JdbcBatchItemWriter" -> {
                    log.debug("Using JdbcBatchWriterBuilder with datasource '{}' for '{}'",
                            config.getConfig().get("datasource"), config.getName());
                    yield JdbcBatchWriterBuilder.build(config, context);
                }
                case "KafkaItemWriter" -> {
                    log.debug("Directly creating KafkaItemWriter for '{}'", config.getName());
                    yield new org.springframework.batch.item.kafka.KafkaItemWriter<>();
                }
                default -> {
                    log.error("Unsupported writer type: '{}'", writerType);
                    throw new TypeNotSupportedException("Unknown writer type: " + writerType);
                }
            };

            log.info("Writer '{}' successfully created for type '{}'", config.getName(), writerType);
            log.debug("Writer '{}' of type '{}' initialized with configuration: {}",
                    config.getName(), writerType, config);

            return writer;

        } catch (Exception e) {
            log.error("Error creating writer '{}' of type '{}': {}",
                    config.getName(), writerType, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Checks whether a bean is compatible with the specified writer type.
     */
    public boolean isAllowedWriter(Object bean, String type) {
        Class<?> expected = WRITER_TYPES.get(type);
        if (expected == null) {
            log.error("Validation failed: unknown writer type '{}'", type);
            throw new TypeNotSupportedException("Unknown writer type: " + type);
        }

        boolean compatible = expected.isInstance(bean);
        log.debug("Writer type check '{}': bean={} compatible={}", type, bean.getClass().getSimpleName(), compatible);
        return compatible;
    }
}
