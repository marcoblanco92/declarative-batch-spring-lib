package com.marbl.declarative_batch.spring_declarative_batch.support.log;

import org.slf4j.MDC;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

public class LoggingStepListener implements StepExecutionListener, ChunkListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        MDC.put("stepName", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        MDC.remove("stepName");
        MDC.remove("chunk");
        return stepExecution.getExitStatus();
    }

    @Override
    public void beforeChunk(ChunkContext context) {
        int chunkNumber = (int) (context.getStepContext().getStepExecution().getCommitCount() + 1);
        MDC.put("chunk", String.valueOf(chunkNumber));
    }

    @Override
    public void afterChunk(ChunkContext context) {
        MDC.remove("chunk");
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        MDC.remove("chunk");
    }
}
