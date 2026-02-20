package com.example.debugdemo;

import com.example.debugdemo.exception.BusinessLogicException;
import com.example.debugdemo.exception.ProductNotFoundException;
import com.example.debugdemo.model.Product;
import com.example.debugdemo.model.ProductStatus;
import com.example.debugdemo.repository.ProductRepository;
import com.example.debugdemo.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Product Service Tests
 * 
 * ═══════════════════════════════════════════════════════════════════
 * DEBUGGING MASTERCLASS - TEST-DRIVEN DEBUGGING
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Tests are excellent for debugging because:
 * 1. They isolate specific functionality
 * 2. You can set breakpoints and debug specific scenarios
 * 3. Reproducible test cases help identify bugs
 * 
 * DEBUGGING TIPS FOR TESTS:
 * 
 * 1. RIGHT-CLICK TEST → DEBUG:
 *    Run individual tests in debug mode
 * 
 * 2. CONDITIONAL BREAKPOINTS:
 *    Stop only when specific conditions are met
 * 
 * 3. WATCH EXPRESSIONS:
 *    Monitor variables as tests run
 * 
 * 4. @DirtiesContext:
 *    Ensures clean database state between tests
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Clear existing data
        productRepository.deleteAll();
        
        // Create a test product
        testProduct = new Product(
                "Test Product",
                "A product for testing",
                new BigDecimal("99.99"),
                50
        );
    }

    // =========================================================
    // CREATE TESTS
    // =========================================================

    @Nested
    @DisplayName("Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully")
        void shouldCreateProduct() {
            // DEBUGGING: Set breakpoint here and step through
            Product created = productService.createProduct(testProduct);

            // Assertions
            assertNotNull(created.getId(), "ID should be generated");
            assertEquals("Test Product", created.getName());
            assertEquals(ProductStatus.ACTIVE, created.getStatus());
            assertNotNull(created.getCreatedAt());
        }

        @Test
        @DisplayName("Should throw exception for duplicate name")
        void shouldThrowForDuplicateName() {
            // Create first product
            productService.createProduct(testProduct);

            // Try to create duplicate
            Product duplicate = new Product(
                    "Test Product",  // Same name!
                    "Different description",
                    new BigDecimal("199.99"),
                    10
            );

            // DEBUGGING: Set breakpoint in validateNewProduct()
            assertThrows(BusinessLogicException.class, () -> {
                productService.createProduct(duplicate);
            });
        }

        @Test
        @DisplayName("Should set default status if not provided")
        void shouldSetDefaultStatus() {
            testProduct.setStatus(null);
            
            Product created = productService.createProduct(testProduct);
            
            assertEquals(ProductStatus.ACTIVE, created.getStatus());
        }
    }

    // =========================================================
    // READ TESTS
    // =========================================================

    @Nested
    @DisplayName("Read Product Tests")
    class ReadProductTests {

        @Test
        @DisplayName("Should get product by ID")
        void shouldGetProductById() {
            Product saved = productService.createProduct(testProduct);

            // DEBUGGING: Check the SQL logs when this runs
            Product found = productService.getProductById(saved.getId());

            assertEquals(saved.getId(), found.getId());
            assertEquals(saved.getName(), found.getName());
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException for invalid ID")
        void shouldThrowForInvalidId() {
            // DEBUGGING: Set breakpoint in GlobalExceptionHandler
            assertThrows(ProductNotFoundException.class, () -> {
                productService.getProductById(99999L);
            });
        }

        @Test
        @DisplayName("Should search products by name")
        void shouldSearchByName() {
            productService.createProduct(testProduct);
            productService.createProduct(new Product("iPhone", "Apple phone", 
                    new BigDecimal("999"), 10));
            productService.createProduct(new Product("Android Phone", "Samsung", 
                    new BigDecimal("899"), 15));

            // DEBUGGING: Check generated SQL for this query
            List<Product> results = productService.searchProducts(
                    "phone", null, null, null);

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Should search products by price range")
        void shouldSearchByPriceRange() {
            productService.createProduct(testProduct); // 99.99
            productService.createProduct(new Product("Expensive", "High price", 
                    new BigDecimal("500"), 5));
            productService.createProduct(new Product("Cheap", "Low price", 
                    new BigDecimal("10"), 100));

            List<Product> results = productService.searchProducts(
                    null, new BigDecimal("50"), new BigDecimal("200"), null);

            // Should only find testProduct (99.99)
            assertEquals(1, results.size());
            assertEquals("Test Product", results.get(0).getName());
        }
    }

    // =========================================================
    // UPDATE TESTS
    // =========================================================

    @Nested
    @DisplayName("Update Product Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product name")
        void shouldUpdateName() {
            Product saved = productService.createProduct(testProduct);

            Product updates = new Product();
            updates.setName("Updated Name");

            // DEBUGGING: Watch how logChanges() works
            Product updated = productService.updateProduct(saved.getId(), updates);

            assertEquals("Updated Name", updated.getName());
            // Price should remain unchanged
            assertEquals(testProduct.getPrice(), updated.getPrice());
        }

        @Test
        @DisplayName("Should update stock correctly")
        void shouldUpdateStock() {
            Product saved = productService.createProduct(testProduct);

            // Reduce stock by 10
            Product updated = productService.updateStock(saved.getId(), -10);

            assertEquals(40, updated.getStock());
        }

        @Test
        @DisplayName("Should throw exception when stock goes negative")
        void shouldThrowForNegativeStock() {
            Product saved = productService.createProduct(testProduct);

            // Try to reduce by more than available
            assertThrows(BusinessLogicException.class, () -> {
                productService.updateStock(saved.getId(), -100);
            });
        }

        @Test
        @DisplayName("Should auto-update status when stock reaches zero")
        void shouldAutoUpdateStatusWhenOutOfStock() {
            Product saved = productService.createProduct(testProduct);

            // Reduce stock to zero
            Product updated = productService.updateStock(saved.getId(), -50);

            assertEquals(0, updated.getStock());
            assertEquals(ProductStatus.OUT_OF_STOCK, updated.getStatus());
        }

        @Test
        @DisplayName("Should auto-restore status when stock replenished")
        void shouldRestoreStatusWhenRestocked() {
            testProduct.setStock(0);
            testProduct.setStatus(ProductStatus.OUT_OF_STOCK);
            Product saved = productService.createProduct(testProduct);

            // Add stock
            Product updated = productService.updateStock(saved.getId(), 10);

            assertEquals(10, updated.getStock());
            assertEquals(ProductStatus.ACTIVE, updated.getStatus());
        }

        @Test
        @DisplayName("Should prevent invalid status transitions")
        void shouldPreventInvalidStatusTransition() {
            testProduct.setStatus(ProductStatus.DISCONTINUED);
            Product saved = productService.createProduct(testProduct);

            Product updates = new Product();
            updates.setStatus(ProductStatus.ACTIVE);

            // Cannot go from DISCONTINUED back to ACTIVE
            assertThrows(BusinessLogicException.class, () -> {
                productService.updateProduct(saved.getId(), updates);
            });
        }
    }

    // =========================================================
    // DELETE TESTS
    // =========================================================

    @Nested
    @DisplayName("Delete Product Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should delete product")
        void shouldDeleteProduct() {
            Product saved = productService.createProduct(testProduct);
            Long id = saved.getId();

            productService.deleteProduct(id);

            assertThrows(ProductNotFoundException.class, () -> {
                productService.getProductById(id);
            });
        }

        @Test
        @DisplayName("Should discontinue product (soft delete)")
        void shouldDiscontinueProduct() {
            Product saved = productService.createProduct(testProduct);

            Product discontinued = productService.discontinueProduct(saved.getId());

            assertEquals(ProductStatus.DISCONTINUED, discontinued.getStatus());
            // Product still exists
            assertNotNull(productService.getProductById(saved.getId()));
        }
    }

    // =========================================================
    // EDGE CASE TESTS
    // =========================================================

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty search results")
        void shouldHandleEmptySearchResults() {
            // No products exist
            List<Product> results = productService.searchProducts(
                    "nonexistent", null, null, null);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should handle all products")
        void shouldHandleAllProducts() {
            productService.createProduct(testProduct);
            productService.createProduct(new Product("Another", "Product", 
                    new BigDecimal("50"), 20));

            List<Product> all = productService.getAllProducts();

            assertEquals(2, all.size());
        }
    }
}
