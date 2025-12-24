package com.marbl.declarative_batch.spring_declarative_batch.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface BulkBatchSteplet {
    String name();
}
