package com.marbl.declarative_batch.spring_declarative_batch.configuration.batch;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ListenerConfig {

    @NotBlank(message = "The Listener name must be provided and cannot be blank")
    private String name;

    @NotBlank(message = "The Listener type must be provided and cannot be blank")
    private String type;



    /**
     * Validation:
     * - if name is null, type must be null
     * - if name is not null, type must be present
     */
    @AssertTrue(message = "'type' must be provided when 'name' is specified, and must be null when 'name' is absent")
    public boolean isValidNameType() {
        if (name == null || name.isBlank()) {
            return type == null || type.isBlank();
        } else {
            return type != null && !type.isBlank();
        }
    }
}
