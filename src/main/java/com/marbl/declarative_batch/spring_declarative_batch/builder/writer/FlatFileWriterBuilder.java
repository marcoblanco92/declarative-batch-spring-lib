package com.marbl.declarative_batch.spring_declarative_batch.builder.writer;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ComponentConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.writer.FlatFileWriterConfig;
import com.marbl.declarative_batch.spring_declarative_batch.utils.MapUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.core.io.FileSystemResource;

/**
 * Builder responsible for creating and configuring a {@link FlatFileItemWriter}
 * based on a declarative {@link ComponentConfig}.
 *
 * <p>Supports configuration of file resource, header/footer callbacks,
 * and field extraction for delimited files.</p>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FlatFileWriterBuilder {

    /**
     * Builds and configures a {@link FlatFileItemWriter} instance.
     *
     * @param config Declarative component configuration
     * @param <O>    Output type handled by the writer
     * @return Configured {@link FlatFileItemWriter}
     */
    public static <O> FlatFileItemWriter<O> build(ComponentConfig config) {
        try {
            log.info("🧩 Building FlatFileItemWriter for component: {}", config.getName());

            // Normalize the map structure (convert numeric-keyed maps to lists)
            Object normalizedMap = MapUtils.normalizeMapStructure(config.getConfig());
            log.debug("Normalized configuration map for FlatFileWriter: {}", normalizedMap);

            // Convert normalized map to configuration DTO
            FlatFileWriterConfig flatConfig = MapUtils.mapToConfigDto(normalizedMap, FlatFileWriterConfig.class);
            log.debug("Mapped configuration to FlatFileWriterConfig DTO: {}", flatConfig);

            // Create writer instance
            FlatFileItemWriter<O> writer = new FlatFileItemWriter<>();
            writer.setName(config.getName());
            writer.setResource(new FileSystemResource(flatConfig.getResource()));
            writer.setAppendAllowed(true);

            // Header
            if (flatConfig.getFileHeader() != null) {
                writer.setHeaderCallback(w -> w.write(flatConfig.getFileHeader()));
                log.debug("Configured file header: {}", flatConfig.getFileHeader());
            }

            // Footer
            if (flatConfig.getFileFooter() != null) {
                writer.setFooterCallback(w -> w.write(flatConfig.getFileFooter()));
                log.debug("Configured file footer: {}", flatConfig.getFileFooter());
            }

            // Line aggregator
            DelimitedLineAggregator<O> lineAggregator = new DelimitedLineAggregator<>();
            lineAggregator.setDelimiter(flatConfig.getDelimiter());

            // Field extractor
            BeanWrapperFieldExtractor<O> fieldExtractor = new BeanWrapperFieldExtractor<>();
            fieldExtractor.setNames(flatConfig.getFieldNames());
            lineAggregator.setFieldExtractor(fieldExtractor);

            writer.setLineAggregator(lineAggregator);
            writer.afterPropertiesSet();

            log.info("Successfully built FlatFileItemWriter for component: {}", config.getName());
            return writer;

        } catch (Exception e) {
            String errorMsg = String.format(
                    "Failed to create FlatFileItemWriter for component '%s': %s",
                    config.getName(),
                    e.getMessage()
            );
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }
}
