package com.marbl.declarative_batch.spring_declarative_batch.configuration.reader;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class FlatFileReaderConfig implements ReaderConfig {

    @NotBlank(message = "'resource' must be provided")
    private String resource;

    private String delimiter = ",";
    private int lineToSkip = 0;

    @NotEmpty(message = "'fieldNames' must contain at least one value")
    private String[] fieldNames;

    @NotBlank(message = "'fieldMapperClass' must be provided")
    private String fieldMapperClass;
}
