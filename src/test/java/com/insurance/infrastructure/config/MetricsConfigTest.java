package com.insurance.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsConfigTest {

    private MetricsConfig metricsConfig;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        metricsConfig = new MetricsConfig();
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void testPolicyCreatedCounter() {
        Counter counter = metricsConfig.policyCreatedCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("insurance.policy.created.total", counter.getId().getName());
        assertEquals("Total number of policy requests created", counter.getId().getDescription());
        assertEquals("policy-service", counter.getId().getTag("service"));
        
        // Test incrementing
        counter.increment();
        assertEquals(1.0, counter.count());
    }

    @Test
    void testPolicyValidatedCounter() {
        Counter counter = metricsConfig.policyValidatedCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("insurance.policy.validated.total", counter.getId().getName());
        assertEquals("Total number of policies validated", counter.getId().getDescription());
        assertEquals("policy-service", counter.getId().getTag("service"));
    }

    @Test
    void testPolicyRejectedCounter() {
        Counter counter = metricsConfig.policyRejectedCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("insurance.policy.rejected.total", counter.getId().getName());
        assertEquals("Total number of policies rejected", counter.getId().getDescription());
        assertEquals("policy-service", counter.getId().getTag("service"));
    }

    @Test
    void testFraudAnalysisCounter() {
        Counter counter = metricsConfig.fraudAnalysisCounter(meterRegistry);
        
        assertNotNull(counter);
        assertEquals("insurance.fraud.analysis.total", counter.getId().getName());
        assertEquals("Total number of fraud analyses performed", counter.getId().getDescription());
        assertEquals("policy-service", counter.getId().getTag("service"));
    }

    @Test
    void testValidationTimer() {
        Timer timer = metricsConfig.validationTimer(meterRegistry);
        
        assertNotNull(timer);
        assertEquals("insurance.policy.validation.duration", timer.getId().getName());
        assertEquals("Time taken to validate a policy", timer.getId().getDescription());
        assertEquals("policy-service", timer.getId().getTag("service"));
        
        // Test timing
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(timer);
        assertTrue(timer.count() > 0);
    }

    @Test
    void testFraudAnalysisTimer() {
        Timer timer = metricsConfig.fraudAnalysisTimer(meterRegistry);
        
        assertNotNull(timer);
        assertEquals("insurance.fraud.analysis.duration", timer.getId().getName());
        assertEquals("Time taken to perform fraud analysis", timer.getId().getDescription());
        assertEquals("policy-service", timer.getId().getTag("service"));
    }

    @Test
    void testActivePoliciesGauge() {
        Gauge gauge = metricsConfig.activePoliciesGauge(meterRegistry);
        
        assertNotNull(gauge);
        assertEquals("insurance.policy.active.count", gauge.getId().getName());
        assertEquals("Number of active policies", gauge.getId().getDescription());
        assertEquals("policy-service", gauge.getId().getTag("service"));
        
        // Test initial value
        assertEquals(0.0, gauge.value());
        
        // Test increment
        metricsConfig.incrementActivePolicies();
        assertEquals(1.0, gauge.value());
        
        // Test decrement
        metricsConfig.decrementActivePolicies();
        assertEquals(0.0, gauge.value());
    }

    @Test
    void testPendingValidationsGauge() {
        Gauge gauge = metricsConfig.pendingValidationsGauge(meterRegistry);
        
        assertNotNull(gauge);
        assertEquals("insurance.policy.pending.validations.count", gauge.getId().getName());
        assertEquals("Number of policies pending validation", gauge.getId().getDescription());
        assertEquals("policy-service", gauge.getId().getTag("service"));
        
        // Test initial value
        assertEquals(0.0, gauge.value());
        
        // Test increment
        metricsConfig.incrementPendingValidations();
        assertEquals(1.0, gauge.value());
        
        // Test decrement
        metricsConfig.decrementPendingValidations();
        assertEquals(0.0, gauge.value());
    }

    @Test
    void testMultipleIncrements() {
        Gauge activePoliciesGauge = metricsConfig.activePoliciesGauge(meterRegistry);
        Gauge pendingValidationsGauge = metricsConfig.pendingValidationsGauge(meterRegistry);
        
        // Multiple increments
        for (int i = 0; i < 5; i++) {
            metricsConfig.incrementActivePolicies();
            metricsConfig.incrementPendingValidations();
        }
        
        assertEquals(5.0, activePoliciesGauge.value());
        assertEquals(5.0, pendingValidationsGauge.value());
        
        // Multiple decrements
        for (int i = 0; i < 3; i++) {
            metricsConfig.decrementActivePolicies();
            metricsConfig.decrementPendingValidations();
        }
        
        assertEquals(2.0, activePoliciesGauge.value());
        assertEquals(2.0, pendingValidationsGauge.value());
    }

    @Test
    void testNegativeValues() {
        Gauge gauge = metricsConfig.activePoliciesGauge(meterRegistry);
        
        // Decrement from 0 should go negative
        metricsConfig.decrementActivePolicies();
        assertEquals(-1.0, gauge.value());
        
        // Increment back to positive
        metricsConfig.incrementActivePolicies();
        metricsConfig.incrementActivePolicies();
        assertEquals(1.0, gauge.value());
    }
} 