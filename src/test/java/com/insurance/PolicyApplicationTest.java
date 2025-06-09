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
        assertDoesNotThrow(() -> {
            PolicyApplication app = new PolicyApplication();
            assertNotNull(app);
        });
    }

    @Test
    void testMainMethodExists() {
        assertDoesNotThrow(() -> {
            String[] args = {};
            PolicyApplication.class.getMethod("main", String[].class);
        });
    }

    @Test
    void testSpringApplicationRunMethod() {
        assertNotNull(PolicyApplication.class.getAnnotation(SpringBootApplication.class));
    }

    @Test
    void testMainMethodWithNullArgs() {
        assertDoesNotThrow(() -> {
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
        SpringBootApplication annotation = PolicyApplication.class.getAnnotation(SpringBootApplication.class);
        assertNotNull(annotation, "PolicyApplication should be annotated with @SpringBootApplication");
        
        assertTrue(annotation.scanBasePackages().length == 0 ||
                  java.util.Arrays.asList(annotation.scanBasePackages()).contains("com.insurance"),
                  "Should scan com.insurance package");
    }

    @Test
    void testApplicationPackage() {
        assertEquals("com.insurance", PolicyApplication.class.getPackageName());
    }

    @Test
    void testApplicationCanBeInstantiatedMultipleTimes() {
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
        PolicyApplication app1 = new PolicyApplication();
        PolicyApplication app2 = new PolicyApplication();
        
        assertNotEquals(app1, app2);
        assertNotEquals(app1.hashCode(), app2.hashCode());
        assertEquals(app1, app1);
    }

    @Test
    void testApplicationToString() {
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
            assertDoesNotThrow(() -> {
                assertTrue(true, "Spring context should load successfully");
            });
        }
    }

    @Test
    void testMainClassConfiguration() {
        assertTrue(PolicyApplication.class.isAnnotationPresent(SpringBootApplication.class));
        
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
        assertDoesNotThrow(() -> {
            try (var mockStatic = mockStatic(SpringApplication.class)) {
                ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);
                
                String[] normalArgs = {"--debug"};
                mockStatic.when(() -> SpringApplication.run(PolicyApplication.class, normalArgs))
                         .thenReturn(mockContext);
                PolicyApplication.main(normalArgs);
                
                String[] multipleArgs = {"--server.port=8080", "--spring.profiles.active=dev"};
                mockStatic.when(() -> SpringApplication.run(PolicyApplication.class, multipleArgs))
                         .thenReturn(mockContext);
                PolicyApplication.main(multipleArgs);
                
                mockStatic.verify(() -> SpringApplication.run(eq(PolicyApplication.class), any(String[].class)), times(2));
            }
        });
    }
} 