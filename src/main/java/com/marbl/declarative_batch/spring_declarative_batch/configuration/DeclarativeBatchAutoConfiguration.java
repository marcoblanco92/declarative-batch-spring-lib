package com.marbl.declarative_batch.spring_declarative_batch.configuration;

import com.marbl.declarative_batch.spring_declarative_batch.utils.ClassNameResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({
        DeclarativeBatchDatasourceAutoConfiguration.class,
        DeclarativeBatchInfrastructureAutoConfiguration.class,
        DeclarativeBatchJobAutoConfiguration.class
})
public class DeclarativeBatchAutoConfiguration {

    @Bean
    public ClassNameResolver classNameResolver(ApplicationContext applicationContext) {
        return new ClassNameResolver(applicationContext);
    }
}
