package com.marbl.declarative_batch.spring_declarative_batch.configuration.batch;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StepConditionConfig {

    private String from;
    @NotBlank(message = "'on' condition must be provided")
    private String onCondition;
    private String toStep;
    private boolean isEnded;

    /**
     * Validation: if onCondition is populated, then toStep must also be populated
     */
    @AssertTrue(message = "'toStep' must be provided if 'onCondition' is set")
    private boolean isValidOnCondition() {
        return onCondition == null || onCondition.isBlank() || (toStep != null && !toStep.isBlank());
    }
}
