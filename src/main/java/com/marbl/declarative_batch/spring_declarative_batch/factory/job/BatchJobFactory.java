package com.marbl.declarative_batch.spring_declarative_batch.factory.job;

import com.marbl.declarative_batch.spring_declarative_batch.annotation.BulkBatchSteplet;
import com.marbl.declarative_batch.spring_declarative_batch.annotation.BulkBatchTasklet;
import com.marbl.declarative_batch.spring_declarative_batch.annotation.BulkBatchValidator;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.*;
import com.marbl.declarative_batch.spring_declarative_batch.exception.BatchException;
import com.marbl.declarative_batch.spring_declarative_batch.factory.step.AbstractSteplet;
import com.marbl.declarative_batch.spring_declarative_batch.support.incrementer.DatabaseRunIdIncrementer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class BatchJobFactory implements JobFactory {

    private final BatchJobConfig jobConfig;
    private final JobRepository jobRepository;
    private final JobExplorer jobExplorer;
    private final ApplicationContext context;
    private final RunIdIncrementer runIdIncrementer;
    private final PlatformTransactionManager transactionManager;

    @Override
    public String getJobName() {
        return jobConfig.getName();
    }

    @Override
    public Job createJob() {
        if (jobConfig == null || jobConfig.getSteps() == null || jobConfig.getSteps().isEmpty()) {
            log.error("Job config is invalid: must contain at least one step");
            throw new IllegalArgumentException("Job config must contain at least one step");
        }

        log.info("Creating job '{}'", jobConfig.getName());

        // --- Create steps dynamically from steplets ---
        Map<String, Step> stepsMap = createStepsFromSteplets(jobConfig);

        // --- Build dynamic flow (supports conditional and next transitions) ---
        Flow mainFlow = buildDynamicFlow(jobConfig, stepsMap);

        // --- Initialize JobBuilder ---
        JobBuilder jobBuilder = new JobBuilder(jobConfig.getName(), jobRepository);

        // --- Attach Job Listener if configured ---
        attachJobListener(jobBuilder, jobConfig);

        // --- Attach Job Parameters Validator if configured ---
        attachJobValidator(jobBuilder, jobConfig);

        // --- Attach RunIdIncrementer if applicable ---
        if (runIdIncrementer != null) {
            log.info("Using RunIdIncrementer: {}", runIdIncrementer.getClass().getSimpleName());
            jobBuilder.incrementer(new DatabaseRunIdIncrementer(jobConfig, jobExplorer));
        }

        // --- Build final Job ---
        Job job = jobBuilder
                .start(mainFlow)
                .end()
                .build();

        log.info("Job '{}' created successfully with {} steps", jobConfig.getName(), stepsMap.size());
        return job;
    }

    // ---------------------------------------------------
    // Helper methods
    // ---------------------------------------------------

    private Map<String, Step> createStepsFromSteplets(BatchJobConfig jobConfig) {
        Map<String, Step> stepsMap = new HashMap<>();

        try {
            for (StepsConfig stepConfig : jobConfig.getSteps()) {
                Step step;

                if (stepConfig.getType() == StepsConfig.StepType.TASKLET) {
                    // --- TASKLET handling ---
                    Tasklet taskletBean = resolveTaskletBean(stepConfig);
                    step = new StepBuilder(stepConfig.getName(), jobRepository)
                            .tasklet(taskletBean, transactionManager)
                            .build();

                    log.info("Tasklet step '{}' created with tasklet bean '{}'",
                            stepConfig.getName(), taskletBean.getClass().getSimpleName());
                } else {
                    // --- Regular STEP (via steplet) ---
                    AbstractSteplet<?, ?> steplet = resolveStepletBean(stepConfig);
                    steplet.setConfig(stepConfig);
                    step = steplet.buildStep();

                    log.info("Chunk-oriented step '{}' created via steplet '{}'",
                            stepConfig.getName(), steplet.getClass().getSimpleName());
                }

                stepsMap.put(stepConfig.getName(), step);
            }
        } catch (Exception e) {
            log.error("Error while creating steps: {}", e.getMessage(), e);
            throw new BatchException("Error while building steps: " + e.getMessage(), e);
        }

        // --- Warn for unused annotated steplets ---
        Map<String, Object> annotatedSteplets = context.getBeansWithAnnotation(BulkBatchSteplet.class);
        for (Object bean : annotatedSteplets.values()) {
            BulkBatchSteplet ann = bean.getClass().getAnnotation(BulkBatchSteplet.class);
            if (!stepsMap.containsKey(ann.name())) {
                log.warn("Annotated steplet bean '{}' ({}) is not used in YAML configuration",
                        ann.name(), bean.getClass().getSimpleName());
            }
        }

        // --- Warn for unused annotated tasklets ---
        Map<String, Object> annotatedTasklets = context.getBeansWithAnnotation(BulkBatchTasklet.class);
        for (Object bean : annotatedTasklets.values()) {
            BulkBatchTasklet ann = bean.getClass().getAnnotation(BulkBatchTasklet.class);
            if (!stepsMap.containsKey(ann.name())) {
                log.warn("Annotated tasklet bean '{}' ({}) is not used in YAML configuration",
                        ann.name(), bean.getClass().getSimpleName());
            }
        }

        return stepsMap;
    }


    private Flow buildDynamicFlow(BatchJobConfig jobConfig, Map<String, Step> stepsMap) {
        FlowBuilder<Flow> flowBuilder = new FlowBuilder<>("flow-" + jobConfig.getName());

        // Start from first step
        Step firstStep = stepsMap.get(jobConfig.getSteps().get(0).getName());
        flowBuilder.start(firstStep);

        for (StepsConfig stepConfig : jobConfig.getSteps()) {
            Step currentStep = stepsMap.get(stepConfig.getName());

            // --- Conditional transitions ---
            if (stepConfig.getTransitions() != null && !stepConfig.getTransitions().isEmpty()) {
                for (StepConditionConfig transition : stepConfig.getTransitions()) {
                    Step toStep = stepsMap.get(transition.getToStep());
                    Step fromStep = stepsMap.get(transition.getFrom());
                    if (toStep == null || fromStep == null) {
                        log.error("Transition references unknown step: {} -> {}", transition.getFrom(), transition.getToStep());
                        throw new IllegalArgumentException("Transition references unknown step: " +
                                transition.getFrom() + " -> " + transition.getToStep());
                    }

                    log.info("Adding conditional transition: {} --[{}]--> {}",
                            fromStep.getName(), transition.getOnCondition(), toStep.getName());

                    flowBuilder.from(fromStep)
                            .on(transition.getOnCondition())
                            .to(toStep);

                    if (transition.isEnded()) {
                        flowBuilder.end();
                    }
                }
            }

            // --- Linear 'next' transitions ---
            if (stepConfig.getNext() != null && !stepConfig.getNext().isEmpty()) {
                Step nextStep = stepsMap.get(stepConfig.getNext());
                if (nextStep == null) {
                    log.error("Next step '{}' not found for step '{}'", stepConfig.getNext(), currentStep.getName());
                    throw new IllegalArgumentException("Next step not found: " + stepConfig.getNext());
                }

                log.info("Adding linear transition: {} --*--> {}", currentStep.getName(), nextStep.getName());

                flowBuilder.from(currentStep)
                        .on("*")
                        .to(nextStep);
            }
        }

        // --- Global fallback for all unhandled exit statuses ---
        for (StepsConfig stepConfig : jobConfig.getSteps()) {
            Step currentStep = stepsMap.get(stepConfig.getName());
            flowBuilder.from(currentStep)
                    .on("*")
                    .end();
        }

        return flowBuilder.end();
    }

    private void attachJobListener(JobBuilder jobBuilder, BatchJobConfig jobConfig) {
        ListenerConfig jobListenerConfig = jobConfig.getListener();
        if (jobListenerConfig == null || jobListenerConfig.getName() == null) {
            log.debug("No JobExecutionListener configured for job '{}'", jobConfig.getName());
            return;
        }

        try {
            JobExecutionListener listener =
                    context.getBean(jobListenerConfig.getName(), JobExecutionListener.class);
            jobBuilder.listener(listener);
            log.info("Attached JobExecutionListener '{}' to job '{}'",
                    jobListenerConfig.getName(), jobConfig.getName());
        } catch (Exception e) {
            log.error("Invalid JobExecutionListener bean '{}': {}", jobListenerConfig.getName(), e.getMessage(), e);
            throw new IllegalArgumentException(
                    "Invalid JobExecutionListener bean: " + jobListenerConfig.getName(), e);
        }
    }

    private void attachJobValidator(JobBuilder jobBuilder, BatchJobConfig jobConfig) {
        ParametersValidatorConfig validatorConfig = jobConfig.getValidator();
        if (validatorConfig == null || !validatorConfig.isValidate()) {
            log.debug("No JobParametersValidator configured for job '{}'", jobConfig.getName());
            return;
        }

        Map<String, Object> validators = context.getBeansWithAnnotation(BulkBatchValidator.class);
        Object validatorBean = validators.get(validatorConfig.getName());

        if (validatorBean == null) {
            log.error("No BulkBatchValidator bean found with name '{}'", validatorConfig.getName());
            throw new IllegalArgumentException("No BulkBatchValidator bean found with name: " + validatorConfig.getName());
        }
        if (!(validatorBean instanceof JobParametersValidator)) {
            log.error("Bean '{}' is not a JobParametersValidator", validatorConfig.getName());
            throw new IllegalArgumentException("Bean '" + validatorConfig.getName() + "' is not a JobParametersValidator");
        }

        jobBuilder.validator((JobParametersValidator) validatorBean);
        log.info("Attached JobParametersValidator '{}' to job '{}'",
                validatorConfig.getName(), jobConfig.getName());
    }

    private AbstractSteplet<?, ?> resolveStepletBean(StepsConfig config) {
        Map<String, Object> beans = context.getBeansWithAnnotation(BulkBatchSteplet.class);
        for (Object bean : beans.values()) {
            BulkBatchSteplet ann = bean.getClass().getAnnotation(BulkBatchSteplet.class);
            if (config.getName().equals(ann.name())) {
                if (!(bean instanceof AbstractSteplet<?, ?>)) {
                    log.error("Bean annotated with @BulkBatchSteplet must extend AbstractSteplet: {}", bean.getClass());
                    throw new IllegalStateException("Bean annotated with @BulkBatchSteplet must extend AbstractSteplet: " + bean.getClass());
                }
                log.debug("Resolved steplet bean '{}' for step '{}'", bean.getClass().getSimpleName(), config.getName());
                return (AbstractSteplet<?, ?>) bean;
            }
        }
        log.error("No steplet bean found for step name '{}'", config.getName());
        throw new IllegalArgumentException("No steplet bean found for step name: " + config.getName());
    }

    private Tasklet resolveTaskletBean(StepsConfig config) {
        Map<String, Object> beans = context.getBeansWithAnnotation(BulkBatchTasklet.class);
        for (Object bean : beans.values()) {
            BulkBatchTasklet ann = bean.getClass().getAnnotation(BulkBatchTasklet.class);
            if (config.getTasklet().equals(ann.name())) {
                if (!(bean instanceof Tasklet)) {
                    log.error("Bean annotated with @BulkBatchTasklet must implement Tasklet: {}", bean.getClass());
                    throw new IllegalStateException("Bean annotated with @BulkBatchTasklet must implement Tasklet: " + bean.getClass());
                }
                log.debug("Resolved tasklet bean '{}' for step '{}'", bean.getClass().getSimpleName(), config.getName());
                return (Tasklet) bean;
            }
        }

        log.error("No tasklet bean found for tasklet name '{}'", config.getTasklet());
        throw new IllegalArgumentException("No tasklet bean found for tasklet name: " + config.getTasklet());
    }

}
