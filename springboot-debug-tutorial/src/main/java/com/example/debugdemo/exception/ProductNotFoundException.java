package com.example.debugdemo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom Exception for Product Not Found
 * 
 * DEBUGGING TIPS FOR EXCEPTIONS:
 * 
 * 1. CREATE SPECIFIC EXCEPTIONS:
 *    - Don't use generic Exception or RuntimeException
 *    - Specific exceptions make stack traces more useful
 *    - Include relevant context (IDs, names, etc.)
 * 
 * 2. USE @ResponseStatus:
 *    - Maps exception to HTTP status automatically
 *    - Makes API debugging easier
 * 
 * 3. INCLUDE CONTEXT IN MESSAGE:
 *    - "Product not found" is unhelpful
 *    - "Product not found with id: 123" tells you what went wrong
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {
    
    private final Long productId;
    private final String productName;

    public ProductNotFoundException(Long id) {
        super(String.format("Product not found with id: %d", id));
        this.productId = id;
        this.productName = null;
    }

    public ProductNotFoundException(String name) {
        super(String.format("Product not found with name: %s", name));
        this.productId = null;
        this.productName = name;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }
}
