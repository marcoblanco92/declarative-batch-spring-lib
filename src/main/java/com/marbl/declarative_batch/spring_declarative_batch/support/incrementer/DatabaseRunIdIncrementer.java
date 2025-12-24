package com.marbl.declarative_batch.spring_declarative_batch.support.incrementer;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.BatchJobConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseRunIdIncrementer implements JobParametersIncrementer {

    private final BatchJobConfig batchJobConfig;
    private final JobExplorer jobExplorer;


    @Override
    public JobParameters getNext(JobParameters parameters) {
        List<JobInstance> instances = jobExplorer.getJobInstances(batchJobConfig.getName(), 0, 1);
        long nextRunId = 1L;

        if (!instances.isEmpty()) {
            JobInstance lastInstance = instances.get(0);
            List<JobExecution> executions = jobExplorer.getJobExecutions(lastInstance);
            if (!executions.isEmpty()) {
                JobParameters lastParams = executions.get(0).getJobParameters();
                Long lastId = lastParams.getLong("run.id");
                if (lastId != null) {
                    nextRunId = lastId + 1;
                }
            }
        }

        return new JobParametersBuilder(parameters)
                .addLong("run.id", nextRunId)
                .toJobParameters();
    }
}