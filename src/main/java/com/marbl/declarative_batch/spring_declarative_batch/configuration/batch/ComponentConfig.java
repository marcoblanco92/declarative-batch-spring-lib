package com.marbl.declarative_batch.spring_declarative_batch.configuration.batch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
public class ComponentConfig {

    @NotEmpty(message = "The Component name must be provided and cannot be empty")
    private String name;
    @NotEmpty(message = "The Component type must be provided and cannot be empty")
    private String type;

    private Map<String, Object> config;

}
