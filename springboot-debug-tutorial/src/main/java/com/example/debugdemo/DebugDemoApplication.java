package com.example.debugdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Main Spring Boot Application
 * 
 * DEBUGGING TIP: This is where the application starts.
 * Set a breakpoint in main() to debug application startup issues.
 */
@SpringBootApplication
public class DebugDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DebugDemoApplication.class);

    public static void main(String[] args) {
        // DEBUGGING TIP: Set a breakpoint here to inspect startup arguments
        logger.info("Starting Debug Demo Application...");
        logger.debug("Arguments received: {}", (Object) args);
        
        SpringApplication.run(DebugDemoApplication.class, args);
    }

    /**
     * This method runs after the application is fully started.
     * DEBUGGING TIP: Good place to verify all beans are loaded correctly.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("=".repeat(60));
        logger.info("DEBUG DEMO APPLICATION STARTED SUCCESSFULLY!");
        logger.info("=".repeat(60));
        logger.info("Useful URLs:");
        logger.info("  - API: http://localhost:8080/api/products");
        logger.info("  - H2 Console: http://localhost:8080/h2-console");
        logger.info("  - Actuator Health: http://localhost:8080/actuator/health");
        logger.info("  - Actuator Beans: http://localhost:8080/actuator/beans");
        logger.info("  - Actuator Mappings: http://localhost:8080/actuator/mappings");
        logger.info("=".repeat(60));
    }
}
