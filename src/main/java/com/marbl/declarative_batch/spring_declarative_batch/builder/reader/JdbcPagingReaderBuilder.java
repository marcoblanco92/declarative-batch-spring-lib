package com.marbl.declarative_batch.spring_declarative_batch.builder.reader;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ComponentConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.reader.JdbcPagingReaderConfig;
import com.marbl.declarative_batch.spring_declarative_batch.utils.DatasourceUtils;
import com.marbl.declarative_batch.spring_declarative_batch.utils.MapUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.Map;

import static com.marbl.declarative_batch.spring_declarative_batch.utils.ReflectionUtils.instantiateClass;

/**
 * Factory builder responsible for creating and configuring {@link JdbcPagingItemReader}
 * instances from declarative {@link ComponentConfig} definitions.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JdbcPagingReaderBuilder {

    /**
     * Builds and configures a {@link JdbcPagingItemReader} using declarative configuration.
     *
     * @param config the component configuration containing reader properties
     * @param context the Spring {@link ApplicationContext} used to resolve dependencies
     * @param chunk the chunk size (page size) for pagination
     * @param <I> the item type
     * @return a configured {@link JdbcPagingItemReader} instance
     */
    public static <I> JdbcPagingItemReader<I> build(ComponentConfig config, ApplicationContext context, int chunk) {
        log.debug("Building JdbcPagingItemReader for component '{}'", config.getName());

        try {
            // Normalize map structure (convert numeric-keyed maps to lists)
            Object normalizedMap = MapUtils.normalizeMapStructure(config.getConfig());
            log.debug("Normalized configuration map: {}", normalizedMap);

            // Convert normalized map into typed configuration DTO
            JdbcPagingReaderConfig jdbcConfig = MapUtils.mapToConfigDto(normalizedMap, JdbcPagingReaderConfig.class);
            log.debug("Mapped JdbcPagingReaderConfig DTO: {}", jdbcConfig);

            // Resolve datasource
            DataSource dataSource = DatasourceUtils.getDataSource(context, jdbcConfig.getDatasource());
            log.debug("Resolved DataSource '{}' for component '{}'", jdbcConfig.getDatasource(), config.getName());

            // Instantiate RowMapper dynamically
            RowMapper<I> rowMapper = instantiateClass(jdbcConfig.getMappedClass(), RowMapper.class);

            // Configure JdbcPagingItemReader
            JdbcPagingItemReader<I> reader = new JdbcPagingItemReader<>();
            reader.setName(config.getName());
            reader.setDataSource(dataSource);
            reader.setRowMapper(rowMapper);
            reader.setPageSize(chunk);

            // Configure query provider and parameters
            PagingQueryProvider queryProvider = jdbcConfig.getProviderType().create(jdbcConfig);
            reader.setQueryProvider(queryProvider);

            Map<String, Object> parameters = jdbcConfig.getParameters();
            reader.setParameterValues(parameters);

            reader.afterPropertiesSet();

            log.info("JdbcPagingItemReader '{}' successfully created with datasource '{}' and page size {}",
                    config.getName(), jdbcConfig.getDatasource(), chunk);

            return reader;

        } catch (ClassNotFoundException e) {
            String errorMsg = String.format(
                    "Invalid JdbcPagingReader configuration: class not found ('%s') for component '%s'",
                    e.getMessage(), config.getName()
            );
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);

        } catch (Exception e) {
            String errorMsg = String.format(
                    "Failed to initialize JdbcPagingItemReader for component '%s': %s",
                    config.getName(), e.getMessage()
            );
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }
}
