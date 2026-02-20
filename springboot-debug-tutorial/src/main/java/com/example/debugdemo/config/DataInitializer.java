package com.example.debugdemo.config;

import com.example.debugdemo.model.Product;
import com.example.debugdemo.model.ProductStatus;
import com.example.debugdemo.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Initializer
 * 
 * DEBUGGING TIP: This class runs on startup.
 * Set a breakpoint in the lambda to debug initial data loading.
 * 
 * If you see "duplicate key" errors, check if data already exists!
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(ProductRepository repository) {
        return args -> {
            logger.info("Initializing sample data...");
            
            // Create sample products
            List<Product> sampleProducts = List.of(
                new Product("iPhone 15 Pro", "Latest Apple smartphone with A17 chip", 
                        new BigDecimal("999.99"), 50),
                new Product("Samsung Galaxy S24", "Flagship Android phone with AI features", 
                        new BigDecimal("899.99"), 75),
                new Product("MacBook Pro 14", "Professional laptop with M3 Pro chip", 
                        new BigDecimal("1999.99"), 25),
                new Product("Sony WH-1000XM5", "Premium noise-cancelling headphones", 
                        new BigDecimal("349.99"), 100),
                new Product("iPad Air", "Versatile tablet for work and play", 
                        new BigDecimal("599.99"), 60),
                new Product("Apple Watch Series 9", "Advanced health and fitness tracker", 
                        new BigDecimal("399.99"), 80),
                new Product("AirPods Pro 2", "Premium wireless earbuds with ANC", 
                        new BigDecimal("249.99"), 150),
                new Product("Nintendo Switch OLED", "Portable gaming console", 
                        new BigDecimal("349.99"), 40),
                new Product("PS5 Controller", "DualSense wireless controller", 
                        new BigDecimal("69.99"), 200),
                new Product("Kindle Paperwhite", "E-reader with glare-free display", 
                        new BigDecimal("139.99"), 90)
            );
            
            // Save all products
            sampleProducts.forEach(product -> {
                try {
                    Product saved = repository.save(product);
                    logger.debug("Created product: id={}, name='{}'", 
                            saved.getId(), saved.getName());
                } catch (Exception e) {
                    logger.error("Failed to create product '{}': {}", 
                            product.getName(), e.getMessage());
                }
            });
            
            // Create one out-of-stock product for testing
            Product outOfStock = new Product("Rare Collector Item", 
                    "Limited edition - currently unavailable", 
                    new BigDecimal("999.99"), 0);
            outOfStock.setStatus(ProductStatus.OUT_OF_STOCK);
            repository.save(outOfStock);
            
            // Create one discontinued product for testing
            Product discontinued = new Product("Legacy Phone XS", 
                    "No longer manufactured", 
                    new BigDecimal("299.99"), 5);
            discontinued.setStatus(ProductStatus.DISCONTINUED);
            repository.save(discontinued);
            
            logger.info("Sample data initialization complete. Total products: {}", 
                    repository.count());
        };
    }
}
