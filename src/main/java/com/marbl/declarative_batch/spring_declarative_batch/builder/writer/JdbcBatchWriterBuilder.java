package com.marbl.declarative_batch.spring_declarative_batch.builder.writer;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ComponentConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.writer.JdbcBatchWriterConfig;
import com.marbl.declarative_batch.spring_declarative_batch.utils.ClassNameResolver;
import com.marbl.declarative_batch.spring_declarative_batch.utils.DatasourceUtils;
import com.marbl.declarative_batch.spring_declarative_batch.utils.MapUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import static com.marbl.declarative_batch.spring_declarative_batch.utils.ReflectionUtils.instantiateClass;

/**
 * Factory builder responsible for creating and configuring {@link JdbcBatchItemWriter}
 * instances based on declarative {@link ComponentConfig} definitions.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JdbcBatchWriterBuilder {

    /**
     * Builds and configures a {@link JdbcBatchItemWriter} using declarative configuration.
     *
     * @param config  the component configuration containing writer properties
     * @param context the Spring {@link ApplicationContext} used to resolve dependencies
     * @param <O>     the output item type
     * @return a configured {@link JdbcBatchItemWriter} instance
     */
    public static <O> JdbcBatchItemWriter<O> build(ComponentConfig config, ApplicationContext context) {
        log.debug("Building JdbcBatchItemWriter for component '{}'", config.getName());

        try {
            // Normalize configuration structure (convert numeric-keyed maps to lists)
            Object normalizedMap = MapUtils.normalizeMapStructure(config.getConfig());
            log.debug("Normalized configuration map: {}", normalizedMap);

            // Map normalized configuration into DTO
            JdbcBatchWriterConfig jdbcConfig = MapUtils.mapToConfigDto(normalizedMap, JdbcBatchWriterConfig.class);
            log.debug("Mapped JdbcBatchWriterConfig DTO: {}", jdbcConfig);

            // Resolve datasource
            DataSource dataSource = DatasourceUtils.getDataSource(context, jdbcConfig.getDatasource());
            log.debug("Resolved DataSource '{}' for component '{}'", jdbcConfig.getDatasource(), config.getName());

            // Get ClassNameResolver bean
            ClassNameResolver classNameResolver = context.getBean(ClassNameResolver.class);

            // Resolve PreparedStatementSetter class using ClassNameResolver
            String preparedStatementClassName = classNameResolver.resolveClass(
                    jdbcConfig.getPreparedStatementClass(),
                    "statement"
            );
            log.debug("Resolved ItemPreparedStatementSetter class: {}", preparedStatementClassName);

            // Instantiate PreparedStatementSetter dynamically
            ItemPreparedStatementSetter<O> psSetter =
                    instantiateClass(preparedStatementClassName, ItemPreparedStatementSetter.class);
            log.debug("Instantiated ItemPreparedStatementSetter of type '{}'", psSetter.getClass().getName());

            // Configure JdbcBatchItemWriter
            JdbcBatchItemWriter<O> writer = new JdbcBatchItemWriter<>();
            writer.setDataSource(dataSource);
            writer.setSql(jdbcConfig.getSql());
            writer.setItemPreparedStatementSetter(psSetter);
            writer.afterPropertiesSet();

            log.info("JdbcBatchItemWriter '{}' successfully created using datasource '{}' and SQL '{}'",
                    config.getName(), jdbcConfig.getDatasource(), jdbcConfig.getSql());

            return writer;

        } catch (ClassNotFoundException e) {
            String errorMsg = String.format(
                    "Invalid JdbcBatchWriter configuration: class not found ('%s') for component '%s'",
                    e.getMessage(), config.getName()
            );
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format(
                    "Failed to initialize JdbcBatchItemWriter for component '%s': %s",
                    config.getName(), e.getMessage()
            );
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }
}