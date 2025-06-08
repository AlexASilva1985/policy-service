package com.insurance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.rabbitmq.host=localhost",
    "logging.level.com.insurance=WARN"
})
@ActiveProfiles("test")
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
        assertNotNull(PolicyApplication.class.getAnnotation(SpringBootApplication.class));
    }

    @Test
    void testMainMethodWithNullArgs() {
        // Testa o método main com args null
        assertDoesNotThrow(() -> {
            // Mock SpringApplication para evitar inicialização real
            try (var mockStatic = mockStatic(SpringApplication.class)) {
                mockStatic.when(() -> SpringApplication.run(PolicyApplication.class, (String[]) null))
                         .thenReturn(mock(ConfigurableApplicationContext.class));
                
                PolicyApplication.main(null);
                
                mockStatic.verify(() -> SpringApplication.run(PolicyApplication.class, (String[]) null));
            }
        });
    }

    @Test
    void testMainMethodWithEmptyArgs() {
        // Testa o método main com array vazio
        assertDoesNotThrow(() -> {
            String[] args = {};
            try (var mockStatic = mockStatic(SpringApplication.class)) {
                mockStatic.when(() -> SpringApplication.run(PolicyApplication.class, args))
                         .thenReturn(mock(ConfigurableApplicationContext.class));
                
                PolicyApplication.main(args);
                
                mockStatic.verify(() -> SpringApplication.run(PolicyApplication.class, args));
            }
        });
    }

    @Test
    void testMainMethodWithArgs() {
        // Testa o método main com argumentos
        assertDoesNotThrow(() -> {
            String[] args = {"--server.port=8081", "--spring.profiles.active=test"};
            try (var mockStatic = mockStatic(SpringApplication.class)) {
                mockStatic.when(() -> SpringApplication.run(PolicyApplication.class, args))
                         .thenReturn(mock(ConfigurableApplicationContext.class));
                
                PolicyApplication.main(args);
                
                mockStatic.verify(() -> SpringApplication.run(PolicyApplication.class, args));
            }
        });
    }

    @Test
    void testApplicationHasCorrectAnnotations() {
        // Verifica se a aplicação tem todas as anotações necessárias
        SpringBootApplication annotation = PolicyApplication.class.getAnnotation(SpringBootApplication.class);
        assertNotNull(annotation, "PolicyApplication should be annotated with @SpringBootApplication");
        
        // Verifica propriedades da anotação
        assertTrue(annotation.scanBasePackages().length == 0 || 
                  java.util.Arrays.asList(annotation.scanBasePackages()).contains("com.insurance"),
                  "Should scan com.insurance package");
    }

    @Test
    void testApplicationPackage() {
        // Verifica se a aplicação está no pacote correto
        assertEquals("com.insurance", PolicyApplication.class.getPackageName());
    }

    @Test
    void testApplicationCanBeInstantiatedMultipleTimes() {
        // Testa se múltiplas instâncias podem ser criadas
        assertDoesNotThrow(() -> {
            PolicyApplication app1 = new PolicyApplication();
            PolicyApplication app2 = new PolicyApplication();
            
            assertNotNull(app1);
            assertNotNull(app2);
            assertNotSame(app1, app2);
        });
    }

    @Test
    void testApplicationHashCodeAndEquals() {
        // Testa equals e hashCode (herdado de Object)
        PolicyApplication app1 = new PolicyApplication();
        PolicyApplication app2 = new PolicyApplication();
        
        assertNotEquals(app1, app2); // Diferentes instâncias
        assertNotEquals(app1.hashCode(), app2.hashCode()); // Diferentes hashCodes
        assertEquals(app1, app1); // Reflexivo
    }

    @Test
    void testApplicationToString() {
        // Testa o método toString
        PolicyApplication app = new PolicyApplication();
        String toString = app.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("PolicyApplication"));
    }

    @SpringBootTest(classes = PolicyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("test")
    static class IntegrationTest {
        
        @Test
        void contextLoads() {
            // Testa se o contexto Spring carrega corretamente
            assertDoesNotThrow(() -> {
                // O contexto é carregado automaticamente pelo @SpringBootTest
                assertTrue(true, "Spring context should load successfully");
            });
        }
    }

    @Test
    void testMainClassConfiguration() {
        // Verifica se a classe está configurada corretamente como main class
        assertTrue(PolicyApplication.class.isAnnotationPresent(SpringBootApplication.class));
        
        // Verifica se o método main é público e estático
        try {
            var mainMethod = PolicyApplication.class.getMethod("main", String[].class);
            assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()));
            assertEquals(void.class, mainMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("Main method should exist with correct signature");
        }
    }

    @Test
    void testSpringApplicationRunCallWithDifferentParameters() {
        // Testa diferentes cenários de chamada do SpringApplication.run
        assertDoesNotThrow(() -> {
            try (var mockStatic = mockStatic(SpringApplication.class)) {
                ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);
                
                // Teste 1: com args normais
                String[] normalArgs = {"--debug"};
                mockStatic.when(() -> SpringApplication.run(PolicyApplication.class, normalArgs))
                         .thenReturn(mockContext);
                PolicyApplication.main(normalArgs);
                
                // Teste 2: com múltiplos args
                String[] multipleArgs = {"--server.port=8080", "--spring.profiles.active=dev"};
                mockStatic.when(() -> SpringApplication.run(PolicyApplication.class, multipleArgs))
                         .thenReturn(mockContext);
                PolicyApplication.main(multipleArgs);
                
                // Verifica que foi chamado 2 vezes
                mockStatic.verify(() -> SpringApplication.run(eq(PolicyApplication.class), any(String[].class)), times(2));
            }
        });
    }
} 