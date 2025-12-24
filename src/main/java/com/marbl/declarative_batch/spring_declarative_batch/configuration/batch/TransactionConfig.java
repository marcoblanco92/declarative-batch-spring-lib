package com.marbl.declarative_batch.spring_declarative_batch.configuration.batch;

import lombok.Data;

import java.util.List;

@Data
public class TransactionConfig {

    private boolean isReaderInTransaction = false;
    private List<Class<? extends Throwable>> noRollbackExceptions; // exceptions to not rollback
}