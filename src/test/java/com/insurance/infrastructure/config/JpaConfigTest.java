package com.insurance.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaConfigTest {

    @Test
    void testJpaConfigClassExists() {
        // Verifica se a classe existe e pode ser instanciada
        JpaConfig jpaConfig = new JpaConfig();
        assertNotNull(jpaConfig);
    }

    @Test
    void testJpaAuditingIsEnabled() {
        // Verifica se a anotação @EnableJpaAuditing está presente
        assertTrue(JpaConfig.class.isAnnotationPresent(EnableJpaAuditing.class));
    }

    @Test
    void testJpaConfigIsConfigurationClass() {
        // Verifica se tem a anotação @Configuration
        assertTrue(JpaConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }
} 