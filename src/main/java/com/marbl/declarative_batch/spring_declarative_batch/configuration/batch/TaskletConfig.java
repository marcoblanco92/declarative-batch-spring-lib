package com.marbl.declarative_batch.spring_declarative_batch.configuration.batch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class TaskletConfig {

    @NotEmpty(message = "The Tasklet name must be provided and cannot be empty")
    private String name;

    private String next;
    @Valid
    private List<StepConditionConfig> transitions;

    // --- Validation for next/transitions ---
    @AssertTrue(message = "Both 'next' and 'transitions' cannot be set simultaneously")
    public boolean isValidTransitions() {
        if (next != null && transitions != null && !transitions.isEmpty()) {
            log.warn("Step [{}] - Both 'next' [{}] and 'transitions' [{}] are set. Only one is allowed.",
                    name, next, transitions);
            return false;
        }
        return true;
    }


}
