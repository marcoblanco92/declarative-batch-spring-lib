package com.marbl.declarative_batch.spring_declarative_batch.configuration.batch;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParametersValidatorConfig {

    @NotBlank(message = "The name must be provided and cannot be blank")
    private String name;

    private boolean validate = false;
}
