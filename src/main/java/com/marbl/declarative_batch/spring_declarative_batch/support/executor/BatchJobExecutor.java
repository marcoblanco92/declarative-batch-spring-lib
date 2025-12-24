package com.marbl.declarative_batch.spring_declarative_batch.support.executor;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.BatchJobConfig;
import com.marbl.declarative_batch.spring_declarative_batch.factory.job.BatchJobFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

@Slf4j
@RequiredArgsConstructor
public class BatchJobExecutor {

    private final BatchJobConfig jobConfig;
    private final BatchJobFactory batchJobFactory;
    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry; // Optional registry for dynamic jobs

    /**
     * Create and run a Spring Batch Job from YAML config
     *
     * @param registerJob true if the job should be registered in JobRegistry (needed for JobOperator)
     * @return JobExecution result
     */
    public JobExecution runJob(JobParameters params, boolean registerJob) {
        try {
            // Create the job dynamically from the JobFactory
            Job job = batchJobFactory.createJob();

            // Conditionally register the job in JobRegistry
            if (registerJob && jobRegistry instanceof MapJobRegistry mapRegistry && !mapRegistry.getJobNames().contains(job.getName())) {
                mapRegistry.register(batchJobFactory);
                log.info("Job '{}' registered in JobRegistry", job.getName());
            }

            JobParameters nextParams = job.getJobParametersIncrementer() != null
                    ? job.getJobParametersIncrementer().getNext(params)
                    : params;

            log.info("JobParameters before increment: {}", params);
            log.info("JobParameters after increment:  {}", nextParams);



            log.info("Launching job '{}' with Params '{}'", job.getName(), params);
            JobExecution execution = jobLauncher.run(job, nextParams);
            log.info("Job '{}' finished successfully with status {}", job.getName(), execution.getStatus());

            return execution;

        } catch (JobExecutionAlreadyRunningException e) {
            log.error("Job '{}' is already running", jobConfig.getName(), e);
        } catch (JobRestartException e) {
            log.error("Job '{}' cannot be restarted", jobConfig.getName(), e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("Job '{}' has already completed", jobConfig.getName(), e);
        } catch (JobParametersInvalidException e) {
            log.error("Job '{}' parameters are invalid", jobConfig.getName(), e);
        } catch (Exception e) {
            log.error("Unexpected error while running job '{}'", jobConfig.getName(), e);
        }

        return null;
    }

    /**
     * Default method without registry registration
     */
    public JobExecution runJob(JobParameters jobParameters) {
        return runJob(jobParameters, false);
    }
}

