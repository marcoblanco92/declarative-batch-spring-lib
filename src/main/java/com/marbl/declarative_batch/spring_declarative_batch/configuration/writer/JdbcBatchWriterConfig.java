package com.marbl.declarative_batch.spring_declarative_batch.configuration.writer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JdbcBatchWriterConfig {

    @NotBlank(message = "'datasource' must be provided")
    private String datasource;

    @NotBlank(message = "'sql' must be provided")
    private String sql;

    @NotBlank(message = "'preparedStatementClass' must be provided")
    private String preparedStatementClass;
}
