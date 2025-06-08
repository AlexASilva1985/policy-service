package com.insurance.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JpaConfigTest {

    @Test
    void testJpaConfigLoads() {
        // Test that JpaConfig can be instantiated successfully
        JpaConfig jpaConfig = new JpaConfig();
        assertNotNull(jpaConfig);
    }

    @Test
    void testJpaConfigIsConfiguration() {
        // Verify that JpaConfig is a proper Spring configuration
        assertTrue(JpaConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    void testJpaConfigHasEnableJpaAuditing() {
        // Verify that JpaConfig enables JPA auditing
        assertTrue(JpaConfig.class.isAnnotationPresent(org.springframework.data.jpa.repository.config.EnableJpaAuditing.class));
    }

    @Test
    void testJpaConfigClass() {
        // Verify class properties
        assertFalse(JpaConfig.class.isInterface());
        assertTrue(java.lang.reflect.Modifier.isPublic(JpaConfig.class.getModifiers()));
    }
} 