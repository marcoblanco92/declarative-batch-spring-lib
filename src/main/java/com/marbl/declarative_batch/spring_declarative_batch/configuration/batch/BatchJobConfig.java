package com.marbl.declarative_batch.spring_declarative_batch.configuration.batch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "bulk.batch-job")
public class BatchJobConfig {

    @NotEmpty(message = "The batch Job name must be provided and cannot be empty")
    private String name;
    @Valid
    private ParametersValidatorConfig validator;
    @Valid
    private ListenerConfig listener;
    @Valid
    @NotEmpty(message = "The batch job must contain at least one step")
    private List<StepsConfig> steps;

}

