package com.marbl.declarative_batch.spring_declarative_batch.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceUtils {

    public static Resource resolveResource(String path) {
        if (path.startsWith("file:")) {
            return new FileSystemResource(path.substring(5));
        } else if (path.startsWith("classpath:")) {
            return new ClassPathResource(path.substring(10));
        } else {
            // default assume classpath
            return new ClassPathResource(path);
        }
    }
}
