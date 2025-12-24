package com.marbl.declarative_batch.spring_declarative_batch.factory.step;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.StepsConfig;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSteplet<I, O> implements StepComponent<I, O> {

    private final StepFactory stepFactory;

    @Setter
    private StepsConfig config;

    /**
     * Build the Spring Batch Step using StepFactory.
     */
    public Step buildStep() throws Exception {
        StepsConfig cfg = getConfig();
        log.info("Building step '{}' via AbstractSteplet '{}'", cfg.getName(), this.getClass().getSimpleName());

        Step step = stepFactory.createStep(
                cfg,
                reader(),
                processor(),
                writer()
        );

        log.info("Step '{}' built successfully via AbstractSteplet '{}'", cfg.getName(), this.getClass().getSimpleName());
        return step;
    }

    /**
     * Get StepsConfig, ensuring it is set.
     */
    protected StepsConfig getConfig() {
        if (config == null) {
            log.error("StepsConfig not set in AbstractSteplet '{}'", this.getClass().getSimpleName());
            throw new IllegalStateException("StepsConfig not set");
        }
        log.debug("Retrieved StepsConfig for step '{}'", config.getName());
        return config;
    }
}
