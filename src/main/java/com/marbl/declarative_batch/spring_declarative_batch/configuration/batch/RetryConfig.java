package com.marbl.declarative_batch.spring_declarative_batch.configuration.batch;

import lombok.Data;

import java.util.List;

@Data
public class RetryConfig {
    private int limit = 3; // default retry limit
    private List<Class<? extends Throwable>> exceptions; // exceptions to retry
}