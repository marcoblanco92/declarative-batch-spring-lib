package com.marbl.declarative_batch.spring_declarative_batch.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtils {

    // Cache for already loaded classes to improve performance
    private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();


    /**
     * Instantiates a class given its name and checks that it implements the expected interface or extends the expected base class.
     *
     * @param className    the fully qualified name of the class to instantiate
     * @param expectedType the interface or base class the class must implement
     * @param <T>          type of the expected interface
     * @return instance of the class
     * @throws IllegalArgumentException if the class cannot be found, does not implement the expected type, or cannot be instantiated
     */
    public static <T> T instantiateClass(String className, Class<T> expectedType) {
        if (!StringUtils.hasText(className)) {
            throw new IllegalArgumentException("Class name must not be empty");
        }

        try {
            // Load class from cache or classloader
            Class<?> clazz = CLASS_CACHE.computeIfAbsent(className, name -> {
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    log.error("Class not found: {}", name, e);
                    throw new IllegalArgumentException("Class not found: " + name, e);
                }
            });

            // Check if the class implements the expected interface
            if (!expectedType.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(
                        String.format("Class %s does not implement %s", className, expectedType.getName())
                );
            }

            // Instantiate the object using the default constructor
            @SuppressWarnings("unchecked")
            T instance = (T) clazz.getDeclaredConstructor().newInstance();
            log.debug("Successfully instantiated class: {}", className);
            return instance;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Error instantiating class %s", className), e
            );
        }
    }
}

