package com.insurance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PolicyApplicationTest {

    @Test
    void testPolicyApplicationClassExists() {
        // Verifica se a classe existe e pode ser instanciada
        assertDoesNotThrow(() -> {
            PolicyApplication app = new PolicyApplication();
            assertNotNull(app);
        });
    }

    @Test
    void testMainMethodExists() {
        // Verifica se o método main existe sem tentar executar Spring Boot
        assertDoesNotThrow(() -> {
            String[] args = {};
            // Apenas verifica se o método existe e é invocável
            PolicyApplication.class.getMethod("main", String[].class);
        });
    }

    @Test
    void testSpringApplicationRunMethod() {
        // Testa se a aplicação tem as anotações corretas
        assertNotNull(PolicyApplication.class.getAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class));
    }
} 