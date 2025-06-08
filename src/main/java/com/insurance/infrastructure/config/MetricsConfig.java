package com.insurance.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuração de métricas customizadas para observabilidade
 */
@Configuration
@Slf4j
public class MetricsConfig {

    private final AtomicLong activePolicies = new AtomicLong(0);
    private final AtomicLong pendingValidations = new AtomicLong(0);

    @Bean
    public Counter policyCreatedCounter(MeterRegistry registry) {
        return Counter.builder("insurance.policy.created.total")
                .description("Total number of policy requests created")
                .tag("service", "policy-service")
                .register(registry);
    }

    @Bean
    public Counter policyValidatedCounter(MeterRegistry registry) {
        return Counter.builder("insurance.policy.validated.total")
                .description("Total number of policies validated")
                .tag("service", "policy-service")
                .register(registry);
    }

    @Bean
    public Counter policyRejectedCounter(MeterRegistry registry) {
        return Counter.builder("insurance.policy.rejected.total")
                .description("Total number of policies rejected")
                .tag("service", "policy-service")
                .register(registry);
    }

    @Bean
    public Counter fraudAnalysisCounter(MeterRegistry registry) {
        return Counter.builder("insurance.fraud.analysis.total")
                .description("Total number of fraud analyses performed")
                .tag("service", "policy-service")
                .register(registry);
    }

    @Bean
    public Timer validationTimer(MeterRegistry registry) {
        return Timer.builder("insurance.policy.validation.duration")
                .description("Time taken to validate a policy")
                .tag("service", "policy-service")
                .register(registry);
    }

    @Bean
    public Timer fraudAnalysisTimer(MeterRegistry registry) {
        return Timer.builder("insurance.fraud.analysis.duration")
                .description("Time taken to perform fraud analysis")
                .tag("service", "policy-service")
                .register(registry);
    }

    @Bean
    public Gauge activePoliciesGauge(MeterRegistry registry) {
        return Gauge.builder("insurance.policy.active.count")
                .description("Number of active policies")
                .tag("service", "policy-service")
                .register(registry, activePolicies, AtomicLong::get);
    }

    @Bean
    public Gauge pendingValidationsGauge(MeterRegistry registry) {
        return Gauge.builder("insurance.policy.pending.validations.count")
                .description("Number of policies pending validation")
                .tag("service", "policy-service")
                .register(registry, pendingValidations, AtomicLong::get);
    }

    // Métodos para atualizar as métricas
    public void incrementActivePolicies() {
        activePolicies.incrementAndGet();
    }

    public void decrementActivePolicies() {
        activePolicies.decrementAndGet();
    }

    public void incrementPendingValidations() {
        pendingValidations.incrementAndGet();
    }

    public void decrementPendingValidations() {
        pendingValidations.decrementAndGet();
    }
} 