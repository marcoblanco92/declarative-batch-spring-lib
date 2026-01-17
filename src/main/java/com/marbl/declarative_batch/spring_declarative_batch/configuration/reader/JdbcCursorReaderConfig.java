package com.marbl.declarative_batch.spring_declarative_batch.configuration.reader;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JdbcCursorReaderConfig implements ReaderConfig {

    @NotBlank(message = "'datasource' must be provided")
    private String datasource;

    @NotBlank(message = "'sql' must be provided")
    private String sql;

    @NotBlank(message = "'preparedStatementClass' must be provided")
    private String preparedStatementClass;

    @NotBlank(message = "'rowMapperClass' must be provided")
    private String rowMapperClass;
}
