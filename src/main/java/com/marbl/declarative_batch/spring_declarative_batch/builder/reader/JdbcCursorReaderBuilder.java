package com.marbl.declarative_batch.spring_declarative_batch.builder.reader;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ComponentConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.reader.JdbcCursorReaderConfig;
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

            // Instantiate RowMapper and PreparedStatementSetter dynamically
            RowMapper<I> rowMapper = ReflectionUtils.instantiateClass(jdbcConfig.getMappedClass(), RowMapper.class);
            PreparedStatementSetter psSetter = ReflectionUtils.instantiateClass(
                    jdbcConfig.getPreparedStatementClass(), PreparedStatementSetter.class
            );

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
