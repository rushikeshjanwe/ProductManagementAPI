package com.example.debugdemo.repository;

import com.example.debugdemo.model.Product;
import com.example.debugdemo.model.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Product Repository
 * 
 * DEBUGGING TIPS FOR REPOSITORIES:
 * 
 * 1. QUERY METHOD NAMING:
 *    - Spring Data JPA generates queries from method names
 *    - If a method fails, check the naming convention
 *    - Enable SQL logging to see generated queries
 * 
 * 2. CUSTOM QUERIES:
 *    - Use @Query for complex queries
 *    - Test queries in H2 console first
 *    - Watch for N+1 problems with relationships
 * 
 * 3. COMMON ISSUES:
 *    - NullPointerException: Check if Optional is handled
 *    - Empty results: Verify data exists, check filters
 *    - Wrong results: Check @Query syntax and parameters
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // =========================================================
    // DERIVED QUERY METHODS
    // Spring generates SQL from method names
    // =========================================================

    /**
     * Find products by name (case-insensitive partial match)
     * DEBUGGING: Check SQL logs to see: SELECT * FROM products WHERE LOWER(name) LIKE LOWER(?)
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Find products by status
     */
    List<Product> findByStatus(ProductStatus status);

    /**
     * Find products in stock (stock > 0)
     */
    List<Product> findByStockGreaterThan(Integer minStock);

    /**
     * Find product by exact name
     */
    Optional<Product> findByName(String name);

    /**
     * Find products within price range
     */
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // =========================================================
    // CUSTOM JPQL QUERIES
    // =========================================================

    /**
     * Custom query with JPQL
     * DEBUGGING TIP: Test this in H2 console to verify results
     */
    @Query("SELECT p FROM Product p WHERE p.status = :status AND p.stock > 0 ORDER BY p.price ASC")
    List<Product> findActiveProductsInStock(@Param("status") ProductStatus status);

    /**
     * Query with multiple conditions
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:status IS NULL OR p.status = :status)")
    List<Product> searchProducts(
            @Param("name") String name,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") ProductStatus status
    );

    // =========================================================
    // NATIVE SQL QUERIES
    // Use when JPQL is not sufficient
    // =========================================================

    /**
     * Native SQL query example
     * DEBUGGING: Native queries bypass JPQL parsing - useful for complex DB-specific queries
     */
    @Query(value = "SELECT * FROM products WHERE stock < :threshold ORDER BY stock ASC", 
           nativeQuery = true)
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    /**
     * Count products by status
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = :status")
    long countByStatus(@Param("status") ProductStatus status);
}
