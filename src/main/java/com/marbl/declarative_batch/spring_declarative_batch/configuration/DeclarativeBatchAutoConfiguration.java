package com.marbl.declarative_batch.spring_declarative_batch.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({
        DeclarativeBatchDatasourceAutoConfiguration.class,
        DeclarativeBatchInfrastructureAutoConfiguration.class,
        DeclarativeBatchJobAutoConfiguration.class
})
public class DeclarativeBatchAutoConfiguration {}
