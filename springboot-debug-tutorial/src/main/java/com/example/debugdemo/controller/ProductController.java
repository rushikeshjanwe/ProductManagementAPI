package com.example.debugdemo.controller;

import com.example.debugdemo.model.Product;
import com.example.debugdemo.model.ProductStatus;
import com.example.debugdemo.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Product REST Controller
 * 
 * ═══════════════════════════════════════════════════════════════════
 * DEBUGGING MASTERCLASS - CONTROLLER LAYER
 * ═══════════════════════════════════════════════════════════════════
 * 
 * CONTROLLER DEBUGGING STRATEGIES:
 * 
 * 1. REQUEST DEBUGGING:
 *    - Log incoming requests with parameters
 *    - Use tools like Postman or curl to test endpoints
 *    - Check request headers if authentication issues
 * 
 * 2. RESPONSE DEBUGGING:
 *    - Log response status and body size
 *    - Use @ResponseStatus for consistent HTTP codes
 *    - Check serialization issues (Jackson annotations)
 * 
 * 3. BREAKPOINT STRATEGIES:
 *    - Set breakpoints at controller methods
 *    - Inspect request parameters and headers
 *    - Watch the flow to service layer
 * 
 * 4. USEFUL ACTUATOR ENDPOINTS:
 *    - /actuator/mappings - shows all endpoints
 *    - /actuator/httptrace - shows recent requests
 *    - /actuator/beans - shows all Spring beans
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
        logger.info("ProductController initialized");
    }

    // =========================================================
    // GET ENDPOINTS
    // =========================================================

    /**
     * Get all products
     * 
     * DEBUGGING EXERCISE #1:
     * 1. Start the app in debug mode
     * 2. Set a breakpoint here
     * 3. Call GET http://localhost:8080/api/products
     * 4. Step into productService.getAllProducts()
     * 5. Watch the data flow through layers
     * 
     * curl -X GET http://localhost:8080/api/products
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        logger.debug("GET /api/products - fetching all products");
        
        long startTime = System.currentTimeMillis();
        List<Product> products = productService.getAllProducts();
        long duration = System.currentTimeMillis() - startTime;
        
        // DEBUGGING TIP: Log performance metrics
        logger.debug("Fetched {} products in {}ms", products.size(), duration);
        
        return ResponseEntity.ok(products);
    }

    /**
     * Get product by ID
     * 
     * DEBUGGING EXERCISE #2:
     * 1. Try to get a non-existent product
     * 2. Watch the exception propagate
     * 3. Check GlobalExceptionHandler
     * 
     * curl -X GET http://localhost:8080/api/products/1
     * curl -X GET http://localhost:8080/api/products/999  # Not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        logger.debug("GET /api/products/{} - fetching product", id);
        
        Product product = productService.getProductById(id);
        
        logger.debug("Returning product: {}", product.getName());
        return ResponseEntity.ok(product);
    }

    /**
     * Search products with filters
     * 
     * DEBUGGING EXERCISE #3 - QUERY PARAMETER DEBUGGING:
     * 1. Set breakpoint and inspect all parameters
     * 2. Watch how null parameters are handled
     * 3. Check SQL logs for generated query
     * 
     * curl "http://localhost:8080/api/products/search?name=phone&minPrice=100"
     */
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) ProductStatus status) {
        
        logger.debug("GET /api/products/search - name='{}', minPrice={}, maxPrice={}, status={}", 
                name, minPrice, maxPrice, status);
        
        List<Product> products = productService.searchProducts(name, minPrice, maxPrice, status);
        
        logger.debug("Search returned {} results", products.size());
        return ResponseEntity.ok(products);
    }

    // =========================================================
    // POST ENDPOINTS
    // =========================================================

    /**
     * Create a new product
     * 
     * DEBUGGING EXERCISE #4 - VALIDATION DEBUGGING:
     * 1. Send invalid data to trigger validation
     * 2. Watch @Valid annotation in action
     * 3. Check ValidationErrorResponse structure
     * 
     * curl -X POST http://localhost:8080/api/products \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"iPhone 15","description":"Latest iPhone","price":999.99,"stock":100}'
     * 
     * # Invalid request (negative price):
     * curl -X POST http://localhost:8080/api/products \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"Bad Product","price":-10,"stock":5}'
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        logger.debug("POST /api/products - creating product: {}", product.getName());
        
        // DEBUGGING TIP: Log the raw request body
        logger.trace("Request body: {}", product);
        
        Product createdProduct = productService.createProduct(product);
        
        logger.info("Created product with id: {}", createdProduct.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    // =========================================================
    // PUT ENDPOINTS
    // =========================================================

    /**
     * Update a product
     * 
     * DEBUGGING EXERCISE #5:
     * 1. Update a product and watch entity state changes
     * 2. Check SQL logs for UPDATE statement
     * 
     * curl -X PUT http://localhost:8080/api/products/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"iPhone 15 Pro","price":1199.99}'
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product productDetails) {
        
        logger.debug("PUT /api/products/{} - updating product", id);
        
        Product updatedProduct = productService.updateProduct(id, productDetails);
        
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * Update product stock
     * 
     * DEBUGGING EXERCISE #6 - BUSINESS LOGIC DEBUGGING:
     * 1. Try to reduce stock below 0
     * 2. Watch BusinessLogicException thrown
     * 3. Check error response structure
     * 
     * curl -X PATCH "http://localhost:8080/api/products/1/stock?change=-5"
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(
            @PathVariable Long id,
            @RequestParam int change) {
        
        logger.debug("PATCH /api/products/{}/stock - change={}", id, change);
        
        Product updatedProduct = productService.updateStock(id, change);
        
        return ResponseEntity.ok(updatedProduct);
    }

    // =========================================================
    // DELETE ENDPOINTS
    // =========================================================

    /**
     * Delete a product
     * 
     * curl -X DELETE http://localhost:8080/api/products/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        logger.debug("DELETE /api/products/{}", id);
        
        productService.deleteProduct(id);
        
        logger.info("Deleted product: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Discontinue a product (soft delete)
     * 
     * curl -X POST http://localhost:8080/api/products/1/discontinue
     */
    @PostMapping("/{id}/discontinue")
    public ResponseEntity<Product> discontinueProduct(@PathVariable Long id) {
        logger.debug("POST /api/products/{}/discontinue", id);
        
        Product product = productService.discontinueProduct(id);
        
        return ResponseEntity.ok(product);
    }

    // =========================================================
    // DEBUG/UTILITY ENDPOINTS
    // =========================================================

    /**
     * Health check endpoint
     * 
     * DEBUGGING TIP: Use this to verify the app is running
     * curl http://localhost:8080/api/products/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Health check called");
        
        Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "service", "ProductController"
        );
        
        return ResponseEntity.ok(health);
    }

    /**
     * Debug info endpoint - shows useful debugging information
     * 
     * DEBUGGING TIP: Useful for quick diagnostics
     * curl http://localhost:8080/api/products/debug-info
     */
    @GetMapping("/debug-info")
    public ResponseEntity<Map<String, Object>> debugInfo() {
        logger.debug("Debug info requested");
        
        List<Product> allProducts = productService.getAllProducts();
        
        Map<String, Object> info = Map.of(
                "totalProducts", allProducts.size(),
                "activeProducts", allProducts.stream()
                        .filter(p -> p.getStatus() == ProductStatus.ACTIVE).count(),
                "outOfStock", allProducts.stream()
                        .filter(p -> p.getStock() == 0).count(),
                "javaVersion", System.getProperty("java.version"),
                "availableProcessors", Runtime.getRuntime().availableProcessors(),
                "freeMemoryMB", Runtime.getRuntime().freeMemory() / (1024 * 1024),
                "totalMemoryMB", Runtime.getRuntime().totalMemory() / (1024 * 1024)
        );
        
        return ResponseEntity.ok(info);
    }
}
