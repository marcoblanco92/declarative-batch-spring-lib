package com.marbl.declarative_batch.spring_declarative_batch.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MapUtils {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper objectMapper = new ObjectMapper(); // for generic conversions

    static {
        yamlMapper.findAndRegisterModules();
        objectMapper.findAndRegisterModules();
    }

    /**
     * Recursively converts numeric-keyed maps into lists.
     *
     * @param obj The object to process
     * @return Converted object
     */
    @SuppressWarnings("unchecked")
    public static Object normalizeMapStructure(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            // Check if all keys are numeric
            if (map.keySet().stream().allMatch(k -> k.toString().matches("\\d+"))) {
                return map.entrySet().stream()
                        .sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getKey().toString())))
                        .map(Map.Entry::getValue)
                        .map(MapUtils::normalizeMapStructure) // recursive call
                        .collect(Collectors.toList());
            } else {
                // Recursive conversion for each value
                Map<String, Object> newMap = new LinkedHashMap<>();
                map.forEach((k, v) -> newMap.put(k.toString(), normalizeMapStructure(v)));
                return newMap;
            }
        } else if (obj instanceof List<?> list) {
            return list.stream()
                    .map(MapUtils::normalizeMapStructure)
                    .collect(Collectors.toList());
        } else {
            return obj;
        }
    }

    /**
     * Converts a map or map-like object into a DTO of the specified type.
     *
     * @param source     The map or object to convert
     * @param targetType The target class type
     * @param <T>        Generic type
     * @return Instance of targetType populated with values
     */
    public static <T> T mapToConfigDto(Object source, Class<T> targetType) {
        Object toMap = normalizeMapStructure(source); // convert numeric-keyed maps into lists
        return objectMapper.convertValue(toMap, targetType);
    }

}
