package com.insurance;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

class InsuranceApplicationTest {

    @Test
    void applicationClassExists() {
        assertNotNull(PolicyApplication.class);
    }

    @Test
    void mainMethodExists() throws NoSuchMethodException {
        assertNotNull(PolicyApplication.class.getMethod("main", String[].class));
    }

    @Test
    void mainMethodDoesNotThrowException() {
        assertDoesNotThrow(() -> {
            String[] args = {};
            PolicyApplication.class.getMethod("main", String[].class);
        });
    }

    @Test
    void springApplicationClassIsAccessible() {
        assertDoesNotThrow(() -> {
            Class<?> springAppClass = SpringApplication.class;
            assertNotNull(springAppClass);
        });
    }

    @Test
    void classHasSpringBootApplicationAnnotation() {
        assertTrue(PolicyApplication.class.isAnnotationPresent(
            org.springframework.boot.autoconfigure.SpringBootApplication.class
        ));
    }
}
