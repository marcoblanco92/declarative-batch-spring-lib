package com.marbl.declarative_batch.spring_declarative_batch.builder.reader;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ComponentConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.reader.JdbcCursorReaderConfig;
import com.marbl.declarative_batch.spring_declarative_batch.utils.ClassNameResolver;
import com.marbl.declarative_batch.spring_declarative_batch.utils.DatasourceUtils;
import com.marbl.declarative_batch.spring_declarative_batch.utils.MapUtils;
import com.marbl.declarative_batch.spring_declarative_batch.utils.ReflectionUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;

/**
 * Factory builder responsible for creating and configuring {@link JdbcCursorItemReader}
 * instances based on declarative {@link ComponentConfig} definitions.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JdbcCursorReaderBuilder {

    /**
     * Builds a fully configured {@link JdbcCursorItemReader} from the provided configuration.
     *
     * @param config  the declarative component configuration
     * @param context the Spring {@link ApplicationContext} to resolve dependencies
     * @param <I>     the target item type
     * @return a configured {@link JdbcCursorItemReader} instance
     */
    public static <I> JdbcCursorItemReader<I> build(ComponentConfig config, ApplicationContext context) {
        log.debug("Building JdbcCursorItemReader for component '{}'", config.getName());

        try {
            // Normalize configuration structure (convert indexed maps, etc.)
            Object normalizedMap = MapUtils.normalizeMapStructure(config.getConfig());
            log.debug("Normalized configuration map: {}", normalizedMap);

            // Map normalized configuration into DTO
            JdbcCursorReaderConfig jdbcConfig = MapUtils.mapToConfigDto(normalizedMap, JdbcCursorReaderConfig.class);
            log.debug("Mapped JdbcCursorReaderConfig DTO: {}", jdbcConfig);

            // Resolve datasource
            DataSource dataSource = DatasourceUtils.getDataSource(context, jdbcConfig.getDatasource());
            log.debug("Resolved DataSource '{}' for component '{}'", jdbcConfig.getDatasource(), config.getName());

            // Get ClassNameResolver bean
            ClassNameResolver classNameResolver = context.getBean(ClassNameResolver.class);

            // Resolve RowMapper class using ClassNameResolver
            String rowMapperClassName = classNameResolver.resolveClass(jdbcConfig.getRowMapperClass(), "mapper");
            log.debug("Resolved RowMapper class: {}", rowMapperClassName);
            RowMapper<I> rowMapper = ReflectionUtils.instantiateClass(rowMapperClassName, RowMapper.class);

            // Resolve PreparedStatementSetter class using ClassNameResolver
            String preparedStatementClassName = classNameResolver.resolveClass(
                    jdbcConfig.getPreparedStatementClass(), "statement"
            );
            log.debug("Resolved PreparedStatementSetter class: {}", preparedStatementClassName);
            PreparedStatementSetter psSetter = ReflectionUtils.instantiateClass(
                    preparedStatementClassName, PreparedStatementSetter.class
            );

            // Build the JdbcCursorItemReader
            JdbcCursorItemReader<I> reader = new JdbcCursorItemReaderBuilder<I>()
                    .name(config.getName())
                    .dataSource(dataSource)
                    .sql(jdbcConfig.getSql())
                    .rowMapper(rowMapper)
                    .preparedStatementSetter(psSetter)
                    .build();

            log.info("JdbcCursorItemReader '{}' successfully created using datasource '{}'",
                    config.getName(), jdbcConfig.getDatasource());

            return reader;

        } catch (ClassNotFoundException e) {
            String errorMsg = String.format(
                    "Invalid JdbcCursorReader configuration: class not found ('%s') for component '%s'",
                    e.getMessage(), config.getName()
            );
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format(
                    "Failed to initialize JdbcCursorItemReader for component '%s': %s",
                    config.getName(), e.getMessage()
            );
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }
}