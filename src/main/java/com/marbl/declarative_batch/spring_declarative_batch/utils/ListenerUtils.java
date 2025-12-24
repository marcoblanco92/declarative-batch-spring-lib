package com.marbl.declarative_batch.spring_declarative_batch.utils;

import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ComponentConfig;
import com.marbl.declarative_batch.spring_declarative_batch.configuration.batch.ListenerConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;

import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ListenerUtils {


    public static boolean validateListenerMatch(
            String stepName,
            ComponentConfig component,
            List<ListenerConfig> listeners,
            String expectedType,
            String role
    ) {
        if (component == null || component.getName() == null || component.getName().isBlank()) {
            return true;
        }
        if (listeners == null || listeners.isEmpty()) {
            return true;
        }

        for (ListenerConfig listener : listeners) {
            if (listener.getName() != null && listener.getType() != null
                    && listener.getType().equalsIgnoreCase(expectedType)
                    && !listener.getName().equals(component.getName())) {
                log.warn("Step [{}] - {} listener name [{}] does not match {} bean name [{}]",
                        stepName, role, listener.getName(), role.toLowerCase(), component.getName());
                return false;
            }

        }
        return true;
    }


    public static Class<?> resolveStepListenerClass(String type) {
        return switch (type) {
            case "StepExecutionListener" -> StepExecutionListener.class;
            case "ChunkListener" -> ChunkListener.class;
            case "ItemReadListener" -> ItemReadListener.class;
            case "ItemProcessListener" -> ItemProcessListener.class;
            case "ItemWriteListener" -> ItemWriteListener.class;
            case "SkipListener" -> SkipListener.class;
            default -> throw new IllegalArgumentException("Unknown listener type: " + type);
        };
    }
}
