package com.marbl.declarative_batch.spring_declarative_batch.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import java.util.Properties;

@Slf4j
@UtilityClass
public class JobParametersUtils {


    /**
     * Converts command-line args into JobParameters.
     * Supports the standard Spring format: --key=value
     */
    public JobParameters fromArgs(String... args) {

        CommandLinePropertySource<?> source = new SimpleCommandLinePropertySource(args);
        Properties props = new Properties();

        for (String name : source.getPropertyNames()) {
            props.put(name, source.getProperty(name));
        }

        DefaultJobParametersConverter converter = new DefaultJobParametersConverter();
        JobParameters jobParameters = converter.getJobParameters(props);

        log.info("Parsed job parameters: {}", jobParameters);
        return jobParameters;
    }
}

