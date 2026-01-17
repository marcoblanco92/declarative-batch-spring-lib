package com.marbl.declarative_batch.spring_declarative_batch.utils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
public class ClassNameResolver {

    private final ApplicationContext applicationContext;

    private String basePackage;

    public ClassNameResolver(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        this.basePackage = resolveBasePackage();
        log.info("ClassNameResolver initialized with base package: {}", basePackage);
    }

    private String resolveBasePackage() {

        // Find @SpringBootApplication annotated bean
        Map<String, Object> annotatedBeans = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
        if (!annotatedBeans.isEmpty()) {
            Object mainBean = annotatedBeans.values().iterator().next();
            String packageName = mainBean.getClass().getPackage().getName();
            log.debug("Detected base package from @SpringBootApplication bean: {}", packageName);
            return packageName;
        }

        // Scan for Main class in classpath
        String detectedPackage = scanForMainClass();
        if (detectedPackage != null) {
            return detectedPackage;
        }

        // Fallback: throw exception if base package cannot be determined
        log.warn("Could not detect base package automatically. Please configure 'declarative-batch.base-package' property");
        throw new IllegalStateException(
                "Base package could not be determined. Please set 'declarative-batch.base-package' in your application properties"
        );
    }

    private String scanForMainClass() {
        try {
            StackTraceElement[] stackTrace = new Exception().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (className.endsWith("Application") || className.endsWith("Main")) {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(SpringBootApplication.class)) {
                        String packageName = clazz.getPackage().getName();
                        log.debug("Detected base package from stack trace scan: {}", packageName);
                        return packageName;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to scan for main class: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Resolves the fully qualified name of a class and validates its existence.
     *
     * @param className the class name (simple or already fully qualified)
     * @param additionalPath the additional path to append to the base package (e.g., "mapper", "processor", "batch.custom")
     * @return the fully qualified class name
     * @throws ClassNotFoundException if the class cannot be found at the resolved path
     *
     * Examples:
     * - resolveClass("MyMapper", "mapper") -> "com.marbl.demo.mapper.MyMapper"
     * - resolveClass("CustomProcessor", "batch.processors") -> "com.marbl.demo.batch.processors.CustomProcessor"
     * - resolveClass("com.example.MyClass", "mapper") -> "com.example.MyClass" (already FQN, returned as-is)
     */
    public String resolveClass(String className, String additionalPath) throws ClassNotFoundException {
        if (!StringUtils.hasText(className)) {
            throw new IllegalArgumentException("className cannot be null or empty");
        }

        String fqn;

        // If it contains a dot, assume it's already a fully qualified name
        if (className.contains(".")) {
            log.debug("Class name '{}' is already fully qualified", className);
            fqn = className;
        } else {
            // Build the FQN: basePackage + additionalPath + className
            fqn = String.format("%s.%s.%s", basePackage, additionalPath, className);
            log.debug("Resolved '{}' with path '{}' to: {}", className, additionalPath, fqn);
        }

        // Validate that the class exists
        try {
            Class.forName(fqn);
            log.debug("Class '{}' successfully validated", fqn);
            return fqn;
        } catch (ClassNotFoundException e) {
            String errorMsg = String.format(
                    "Class '%s' not found at path '%s.%s'. Expected fully qualified name: '%s'",
                    className, basePackage, additionalPath, fqn
            );
            log.error(errorMsg);
            throw new ClassNotFoundException(errorMsg, e);
        }
    }

    public String getBasePackage() {
        return basePackage;
    }
}