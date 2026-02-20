package com.example.debugdemo.service;

import com.example.debugdemo.exception.BusinessLogicException;
import com.example.debugdemo.exception.ProductNotFoundException;
import com.example.debugdemo.model.Product;
import com.example.debugdemo.model.ProductStatus;
import com.example.debugdemo.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Product Service
 * 
 * ═══════════════════════════════════════════════════════════════════
 * DEBUGGING MASTERCLASS - SERVICE LAYER
 * ═══════════════════════════════════════════════════════════════════
 * 
 * The service layer is where most business logic bugs occur.
 * This class demonstrates various debugging techniques.
 * 
 * KEY DEBUGGING STRATEGIES:
 * 
 * 1. STRATEGIC LOGGING:
 *    - Log method entry with parameters
 *    - Log important decisions/branches
 *    - Log method exit with results
 *    - Use appropriate log levels
 * 
 * 2. BREAKPOINT STRATEGIES:
 *    - Set breakpoints at method entry
 *    - Use conditional breakpoints for specific conditions
 *    - Use "Evaluate Expression" to inspect complex objects
 * 
 * 3. TRANSACTION DEBUGGING:
 *    - Watch for transaction boundaries
 *    - Check for rollback conditions
 *    - Monitor connection pool
 */
@Service
@Transactional
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    @Value("${app.debug.simulate-slow-queries:false}")
    private boolean simulateSlowQueries;

    @Value("${app.debug.simulate-random-errors:false}")
    private boolean simulateRandomErrors;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
        logger.info("ProductService initialized");
    }

    // =========================================================
    // CREATE OPERATIONS
    // =========================================================

    /**
     * Create a new product
     * 
     * DEBUGGING EXERCISE #1:
     * 1. Set a breakpoint at the start of this method
     * 2. Call the API to create a product
     * 3. Step through each line (F8 in IntelliJ)
     * 4. Inspect the 'product' object before and after save
     * 5. Notice how 'id' is null before save, populated after
     */
    public Product createProduct(Product product) {
        logger.debug(">>> createProduct() called with: {}", product);
        
        // Simulate slow query if enabled (for timeout debugging)
        simulateSlowQueryIfEnabled();
        
        // Validate business rules
        validateNewProduct(product);
        
        // Set initial status
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
            logger.debug("Set default status to ACTIVE");
        }
        
        // Save and log result
        Product savedProduct = productRepository.save(product);
        
        logger.info("Created product: id={}, name='{}'", 
                savedProduct.getId(), savedProduct.getName());
        logger.debug("<<< createProduct() returning: {}", savedProduct);
        
        return savedProduct;
    }

    // =========================================================
    // READ OPERATIONS
    // =========================================================

    /**
     * Get all products
     * 
     * DEBUGGING EXERCISE #2:
     * 1. Set a breakpoint here
     * 2. Use "Evaluate Expression" (Alt+F8) to run:
     *    - productRepository.count()
     *    - products.stream().filter(p -> p.getPrice().compareTo(BigDecimal.TEN) > 0).count()
     */
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        logger.debug(">>> getAllProducts() called");
        
        List<Product> products = productRepository.findAll();
        
        logger.debug("<<< getAllProducts() returning {} products", products.size());
        return products;
    }

    /**
     * Get product by ID
     * 
     * DEBUGGING EXERCISE #3:
     * 1. Try to get a product that doesn't exist
     * 2. Watch the exception flow through GlobalExceptionHandler
     * 3. Check the logs for the error ID
     */
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        logger.debug(">>> getProductById() called with id={}", id);
        
        // Simulate random error if enabled (for retry/resilience debugging)
        simulateRandomErrorIfEnabled();
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Product not found with id: {}", id);
                    return new ProductNotFoundException(id);
                });
        
        logger.debug("<<< getProductById() returning: {}", product);
        return product;
    }

    /**
     * Search products with multiple criteria
     * 
     * DEBUGGING EXERCISE #4 - COMPLEX QUERY DEBUGGING:
     * 1. Set a breakpoint here
     * 2. Try different combinations of parameters
     * 3. Enable SQL logging to see generated queries
     * 4. Use H2 console to run the same query manually
     */
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String name, BigDecimal minPrice, 
                                         BigDecimal maxPrice, ProductStatus status) {
        logger.debug(">>> searchProducts() called with name='{}', minPrice={}, maxPrice={}, status={}", 
                name, minPrice, maxPrice, status);
        
        List<Product> results = productRepository.searchProducts(name, minPrice, maxPrice, status);
        
        logger.debug("<<< searchProducts() returning {} results", results.size());
        
        // DEBUGGING TIP: Log sample results for verification
        if (logger.isTraceEnabled() && !results.isEmpty()) {
            logger.trace("Sample result: {}", results.get(0));
        }
        
        return results;
    }

    // =========================================================
    // UPDATE OPERATIONS
    // =========================================================

    /**
     * Update a product
     * 
     * DEBUGGING EXERCISE #5 - ENTITY STATE DEBUGGING:
     * 1. Set breakpoints at: findById, each setter, save
     * 2. Watch the entity state change
     * 3. Notice JPA dirty checking - updates only changed fields
     * 4. Check SQL logs to see the actual UPDATE statement
     */
    public Product updateProduct(Long id, Product productDetails) {
        logger.debug(">>> updateProduct() called with id={}, details={}", id, productDetails);
        
        Product existingProduct = getProductById(id);
        
        // Log what's changing
        logChanges(existingProduct, productDetails);
        
        // Apply updates
        if (productDetails.getName() != null) {
            existingProduct.setName(productDetails.getName());
        }
        if (productDetails.getDescription() != null) {
            existingProduct.setDescription(productDetails.getDescription());
        }
        if (productDetails.getPrice() != null) {
            validatePrice(productDetails.getPrice());
            existingProduct.setPrice(productDetails.getPrice());
        }
        if (productDetails.getStock() != null) {
            existingProduct.setStock(productDetails.getStock());
        }
        if (productDetails.getStatus() != null) {
            validateStatusTransition(existingProduct.getStatus(), productDetails.getStatus());
            existingProduct.setStatus(productDetails.getStatus());
        }
        
        Product updatedProduct = productRepository.save(existingProduct);
        
        logger.info("Updated product: id={}", id);
        logger.debug("<<< updateProduct() returning: {}", updatedProduct);
        
        return updatedProduct;
    }

    /**
     * Update stock (demonstrates atomic operations)
     * 
     * DEBUGGING EXERCISE #6 - CONCURRENCY DEBUGGING:
     * 1. This method modifies stock atomically
     * 2. Test with multiple concurrent requests
     * 3. Watch for race conditions
     */
    public Product updateStock(Long id, int quantityChange) {
        logger.debug(">>> updateStock() called with id={}, change={}", id, quantityChange);
        
        Product product = getProductById(id);
        int newStock = product.getStock() + quantityChange;
        
        // Business rule validation
        if (newStock < 0) {
            logger.warn("Stock update would result in negative stock: current={}, change={}, result={}", 
                    product.getStock(), quantityChange, newStock);
            throw new BusinessLogicException("INSUFFICIENT_STOCK",
                    "Cannot reduce stock below 0. Current: " + product.getStock() + 
                    ", Requested change: " + quantityChange);
        }
        
        product.setStock(newStock);
        
        // Auto-update status based on stock
        if (newStock == 0 && product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
            logger.info("Product {} is now out of stock", id);
        } else if (newStock > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
            logger.info("Product {} is back in stock", id);
        }
        
        Product savedProduct = productRepository.save(product);
        
        logger.debug("<<< updateStock() returning: {}", savedProduct);
        return savedProduct;
    }

    // =========================================================
    // DELETE OPERATIONS
    // =========================================================

    /**
     * Delete a product
     */
    public void deleteProduct(Long id) {
        logger.debug(">>> deleteProduct() called with id={}", id);
        
        Product product = getProductById(id);
        productRepository.delete(product);
        
        logger.info("Deleted product: id={}, name='{}'", id, product.getName());
        logger.debug("<<< deleteProduct() completed");
    }

    /**
     * Soft delete (mark as discontinued)
     * 
     * DEBUGGING TIP: Soft deletes preserve data for debugging historical issues
     */
    public Product discontinueProduct(Long id) {
        logger.debug(">>> discontinueProduct() called with id={}", id);
        
        Product product = getProductById(id);
        product.setStatus(ProductStatus.DISCONTINUED);
        Product savedProduct = productRepository.save(product);
        
        logger.info("Discontinued product: id={}", id);
        return savedProduct;
    }

    // =========================================================
    // VALIDATION METHODS
    // =========================================================

    private void validateNewProduct(Product product) {
        logger.trace("Validating new product: {}", product.getName());
        
        // Check for duplicate names
        Optional<Product> existing = productRepository.findByName(product.getName());
        if (existing.isPresent()) {
            logger.warn("Duplicate product name: '{}'", product.getName());
            throw new BusinessLogicException("DUPLICATE_NAME",
                    "Product with name '" + product.getName() + "' already exists");
        }
        
        validatePrice(product.getPrice());
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessLogicException("INVALID_PRICE",
                    "Price must be greater than 0");
        }
    }

    private void validateStatusTransition(ProductStatus from, ProductStatus to) {
        // DEBUGGING EXERCISE #7: Add breakpoint here to debug state machine
        logger.trace("Validating status transition: {} -> {}", from, to);
        
        // Example: Can't go from DISCONTINUED back to ACTIVE
        if (from == ProductStatus.DISCONTINUED && to == ProductStatus.ACTIVE) {
            throw new BusinessLogicException("INVALID_STATUS_TRANSITION",
                    "Cannot reactivate a discontinued product");
        }
    }

    // =========================================================
    // DEBUGGING HELPER METHODS
    // =========================================================

    /**
     * Log changes between existing and new product
     * DEBUGGING TIP: This helps trace what changed during updates
     */
    private void logChanges(Product existing, Product updated) {
        if (logger.isDebugEnabled()) {
            StringBuilder changes = new StringBuilder("Changes for product " + existing.getId() + ": ");
            boolean hasChanges = false;
            
            if (updated.getName() != null && !updated.getName().equals(existing.getName())) {
                changes.append(String.format("name['%s'->'%s'] ", existing.getName(), updated.getName()));
                hasChanges = true;
            }
            if (updated.getPrice() != null && !updated.getPrice().equals(existing.getPrice())) {
                changes.append(String.format("price[%s->%s] ", existing.getPrice(), updated.getPrice()));
                hasChanges = true;
            }
            if (updated.getStock() != null && !updated.getStock().equals(existing.getStock())) {
                changes.append(String.format("stock[%d->%d] ", existing.getStock(), updated.getStock()));
                hasChanges = true;
            }
            if (updated.getStatus() != null && !updated.getStatus().equals(existing.getStatus())) {
                changes.append(String.format("status[%s->%s] ", existing.getStatus(), updated.getStatus()));
                hasChanges = true;
            }
            
            if (hasChanges) {
                logger.debug(changes.toString());
            } else {
                logger.debug("No changes detected for product {}", existing.getId());
            }
        }
    }

    /**
     * Simulate slow query for timeout debugging
     */
    private void simulateSlowQueryIfEnabled() {
        if (simulateSlowQueries) {
            try {
                logger.warn("SIMULATION: Adding 3 second delay to simulate slow query");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Simulate random errors for resilience testing
     */
    private void simulateRandomErrorIfEnabled() {
        if (simulateRandomErrors && Math.random() < 0.3) {
            logger.warn("SIMULATION: Throwing random error for resilience testing");
            throw new RuntimeException("Simulated random error for debugging");
        }
    }
}
