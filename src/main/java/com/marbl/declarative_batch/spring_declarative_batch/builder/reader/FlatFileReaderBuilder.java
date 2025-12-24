package com.marbl.declarative_batch.spring_declarative_batch.builder.reader;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ComponentConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.reader.FlatFileReaderConfig;
import com.marbl.declarative_batch.spring_declarative_batch.utils.MapUtils;
import com.marbl.declarative_batch.spring_declarative_batch.utils.ResourceUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.Resource;

/**
 * Factory builder responsible for creating and configuring {@link FlatFileItemReader}
 * instances from declarative {@link ComponentConfig} definitions.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FlatFileReaderBuilder {

    /**
     * Builds a fully configured {@link FlatFileItemReader} instance based on the provided {@link ComponentConfig}.
     *
     * @param config The declarative component configuration
     * @param <I>    The target item type
     * @return Configured {@link FlatFileItemReader} instance
     */
    public static <I> FlatFileItemReader<I> build(ComponentConfig config) {
        log.debug("Building FlatFileItemReader for component '{}'", config.getName());

        // Normalize nested map structures (convert indexed maps into lists, etc.)
        Object normalizedMap = MapUtils.normalizeMapStructure(config.getConfig());
        log.debug("Normalized configuration map: {}", normalizedMap);

        // Map normalized configuration into DTO
        FlatFileReaderConfig flatConfig = MapUtils.mapToConfigDto(normalizedMap, FlatFileReaderConfig.class);
        log.debug("Mapped FlatFileReaderConfig DTO: {}", flatConfig);

        try {
            FlatFileItemReader<I> reader = new FlatFileItemReader<>();
            reader.setName(config.getName());

            Resource resource = ResourceUtils.resolveResource(flatConfig.getResource());
            reader.setResource(resource);
            reader.setLinesToSkip(flatConfig.getLineToSkip());

            // Configure LineMapper
            DefaultLineMapper<I> lineMapper = new DefaultLineMapper<>();

            DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
            tokenizer.setDelimiter(flatConfig.getDelimiter());
            tokenizer.setNames(flatConfig.getFieldNames());

            BeanWrapperFieldSetMapper<I> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
            @SuppressWarnings("unchecked")
            Class<I> targetClass = (Class<I>) Class.forName(flatConfig.getMappedClass());
            fieldSetMapper.setTargetType(targetClass);

            lineMapper.setLineTokenizer(tokenizer);
            lineMapper.setFieldSetMapper(fieldSetMapper);
            reader.setLineMapper(lineMapper);

            reader.afterPropertiesSet();

            log.info("FlatFileItemReader '{}' successfully created for resource '{}'",
                    config.getName(), flatConfig.getResource());
            return reader;

        } catch (ClassNotFoundException e) {
            String errorMsg = String.format(
                    "Invalid FlatFileReader configuration: mapped class '%s' not found for component '%s'",
                    flatConfig.getMappedClass(), config.getName()
            );
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);

        } catch (Exception e) {
            String errorMsg = String.format(
                    "Failed to initialize FlatFileItemReader for component '%s': %s",
                    config.getName(), e.getMessage()
            );
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }
}
