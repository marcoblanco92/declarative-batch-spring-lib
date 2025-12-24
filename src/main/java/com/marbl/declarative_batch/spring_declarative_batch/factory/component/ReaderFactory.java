package com.marbl.declarative_batch.spring_declarative_batch.factory.component;

import com.marbl.declarative_batch.spring_declarative_batch.builder.reader.FlatFileReaderBuilder;
import com.marbl.declarative_batch.spring_declarative_batch.builder.reader.JdbcCursorReaderBuilder;
import com.marbl.declarative_batch.spring_declarative_batch.builder.reader.JdbcPagingReaderBuilder;
import com.marbl.declarative_batch.spring_declarative_batch.exception.TypeNotSupportedException;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ComponentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
public class ReaderFactory {

    private final ApplicationContext context;

    private static final Map<String, Class<?>> READER_TYPES = Map.of(
            "FlatFileItemReader", FlatFileItemReader.class,
            "JdbcPagingItemReader", org.springframework.batch.item.database.JdbcPagingItemReader.class,
            "JdbcCursorItemReader", JdbcCursorItemReader.class,
            "MongoCursorItemReader", org.springframework.batch.item.data.MongoCursorItemReader.class,
            "ItemReader", ItemReader.class
    );

    public ReaderFactory(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Creates a typed ItemReader<I> based on configuration or Spring context.
     */
    @SuppressWarnings("unchecked")
    public <I> ItemReader<I> createReader(ComponentConfig config, int chunk) throws Exception {

        if (!StringUtils.hasText(config.getType())) {
            log.error("Reader creation failed: 'type' field is empty in ComponentConfig");
            throw new IllegalArgumentException("Reader type must be provided");
        }

        String readerType = config.getType();
        log.debug("Starting reader creation: type='{}' for component '{}'", readerType, config.getName());

        ItemReader<I> reader;
        try {
            reader = switch (readerType) {
                case "FlatFileItemReader" -> {
                    log.debug("Using FlatFileReaderBuilder for component '{}'", config.getName());
                    yield FlatFileReaderBuilder.build(config);
                }
                case "JdbcCursorItemReader" -> {
                    log.debug("Using JdbcCursorReaderBuilder for component '{}'", config.getName());
                    yield JdbcCursorReaderBuilder.build(config, context);
                }
                case "JdbcPagingItemReader" -> {
                    log.debug("Using JdbcPagingReaderBuilder with chunk size '{}' for component '{}'", chunk, config.getName());
                    yield JdbcPagingReaderBuilder.build(config, context, chunk);
                }
                // case "MongoCursorItemReader" -> {
                //     log.debug("Using MongoCursorReaderBuilder for component '{}'", config.getName());
                //     yield MongoCursorReaderBuilder.<I>build(config, context);
                // }
                default -> {
                    log.error("Unsupported reader type: '{}'", readerType);
                    throw new TypeNotSupportedException("Unknown reader type: " + readerType);
                }
            };

            log.info("Reader '{}' successfully created for type '{}'", config.getName(), readerType);
            log.debug("Reader '{}' of type '{}' initialized with configuration: {}",
                    config.getName(), readerType, config);

            return reader;

        } catch (Exception e) {
            log.error("Error creating reader '{}' of type '{}': {}",
                    config.getName(), readerType, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Checks whether a bean is compatible with the specified reader type.
     */
    public boolean isAllowedReader(Object bean, String type) {
        Class<?> expected = READER_TYPES.get(type);
        if (expected == null) {
            log.error("Validation failed: unknown reader type '{}'", type);
            throw new TypeNotSupportedException("Unknown reader type: " + type);
        }

        boolean compatible = expected.isInstance(bean);
        log.debug("Reader type check '{}': bean={} compatible={}", type, bean.getClass().getSimpleName(), compatible);
        return compatible;
    }
}
