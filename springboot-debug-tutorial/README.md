# Spring Boot Debugging Tutorial

A comprehensive project designed to teach you how to debug large Spring Boot applications effectively.

## Quick Start

```bash
# Navigate to project directory
cd springboot-debug-tutorial

# Run with Maven
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package
java -jar target/debug-demo-1.0.0.jar
```




**Access Points:**
- API: http://localhost:8080/api/products
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:debugdb`)
- Actuator Health: http://localhost:8080/actuator/health
- Actuator Mappings: http://localhost:8080/actuator/mappings

---

## Table of Contents

1. [IDE Setup for Debugging](#1-ide-setup-for-debugging)
2. [Breakpoint Strategies](#2-breakpoint-strategies)
3. [Logging Best Practices](#3-logging-best-practices)
4. [Request Tracing](#4-request-tracing)
5. [Database Debugging](#5-database-debugging)
6. [Exception Debugging](#6-exception-debugging)
7. [Performance Debugging](#7-performance-debugging)
8. [Actuator Endpoints](#8-actuator-endpoints)
9. [Common Issues & Solutions](#9-common-issues--solutions)
10. [Debugging Exercises](#10-debugging-exercises)

---

## 1. IDE Setup for Debugging

### IntelliJ IDEA

**Start in Debug Mode:**
1. Open `DebugDemoApplication.java`
2. Click the green bug icon (🪲) next to `main()`
3. Or press `Shift + F9`

**Key Shortcuts:**
| Action | Shortcut |
|--------|----------|
| Step Over | F8 |
| Step Into | F7 |
| Step Out | Shift + F8 |
| Resume | F9 |
| Evaluate Expression | Alt + F8 |
| Toggle Breakpoint | Ctrl + F8 |
| View Breakpoints | Ctrl + Shift + F8 |

### VS Code

1. Install "Spring Boot Extension Pack"
2. Open the project
3. Press F5 or use "Run and Debug" panel
4. Select "Java" debugger

### Remote Debugging

Run the application with debug agent:

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
     -jar target/debug-demo-1.0.0.jar
```

Then connect your IDE to `localhost:5005`.

---

## 2. Breakpoint Strategies

### Types of Breakpoints

**Line Breakpoints** (Most Common)
- Click the gutter next to a line number
- Execution pauses when this line is about to execute

**Conditional Breakpoints** (Very Useful!)
- Right-click on a breakpoint → Add condition
- Example: `id == 5` - only stops when id equals 5
- Example: `product.getPrice().compareTo(BigDecimal.valueOf(100)) > 0`

**Exception Breakpoints**
- In IntelliJ: Run → View Breakpoints → Add → Java Exception Breakpoints
- Add `NullPointerException` to catch all NPEs
- Add `ProductNotFoundException` to catch business exceptions

**Method Breakpoints**
- Set on method signature to break on entry/exit
- Useful for tracking method calls

### Strategic Breakpoint Locations

```
┌─────────────────────────────────────────────────────────────┐
│  1. CONTROLLER LAYER                                         │
│     └── Set breakpoints to inspect incoming requests         │
│         - Check @RequestBody deserialization                 │
│         - Verify @PathVariable and @RequestParam values      │
│                                                              │
│  2. SERVICE LAYER                                            │
│     └── Set breakpoints for business logic                   │
│         - Validate method inputs                             │
│         - Check business rule conditions                     │
│         - Inspect data transformations                       │
│                                                              │
│  3. REPOSITORY LAYER                                         │
│     └── Set breakpoints to verify DB operations              │
│         - Check query parameters                             │
│         - Verify returned entities                           │
│                                                              │
│  4. EXCEPTION HANDLER                                        │
│     └── Set breakpoints to catch all errors                  │
│         - GlobalExceptionHandler is perfect for this         │
└─────────────────────────────────────────────────────────────┘
```

### Recommended Breakpoint Locations in This Project

```java
// ProductController.java - Line ~70 (createProduct method)
// Watch incoming product data

// ProductService.java - Line ~95 (createProduct method)  
// Watch business logic execution

// ProductRepository.java - Any custom query method
// Use conditional breakpoint: results.size() == 0

// GlobalExceptionHandler.java - Line ~85 (handleAllExceptions)
// Catch ALL unexpected errors
```

---

## 3. Logging Best Practices

### Log Levels Explained

```
TRACE  →  Most detailed, for tracing code paths
DEBUG  →  Detailed info for debugging
INFO   →  General operational messages
WARN   →  Potential problems
ERROR  →  Errors that need attention
```

### Logging Pattern Used in This Project

```java
// Method entry (DEBUG level)
logger.debug(">>> methodName() called with param={}", param);

// Important decisions (DEBUG level)
logger.debug("Taking branch A because condition={}", condition);

// Method exit (DEBUG level)
logger.debug("<<< methodName() returning: {}", result);

// Operations (INFO level)
logger.info("Created product: id={}, name='{}'", id, name);

// Warnings (WARN level)
logger.warn("Product not found with id: {}", id);

// Errors (ERROR level)
logger.error("Failed to process: {}", exception.getMessage(), exception);
```

### Change Log Levels at Runtime

**Option 1: application.yml**
```yaml
logging:
  level:
    com.example.debugdemo: DEBUG
    org.hibernate.SQL: DEBUG
```

**Option 2: Actuator Endpoint**
```bash
# Check current level
curl http://localhost:8080/actuator/loggers/com.example.debugdemo

# Change level at runtime
curl -X POST http://localhost:8080/actuator/loggers/com.example.debugdemo \
     -H "Content-Type: application/json" \
     -d '{"configuredLevel": "TRACE"}'
```

---

## 4. Request Tracing

### Request ID Correlation

Every request gets a unique ID (see `RequestLoggingFilter`):

```
[A1B2C3D4] --> POST /api/products
[A1B2C3D4] Created product: id=15, name='New Phone'
[A1B2C3D4] <-- 201 CREATED (45ms)
```

Find all logs for a request:
```bash
grep "A1B2C3D4" logs/debug-demo.log
```

### HTTP Request/Response Logging

The `RequestLoggingFilter` logs:
- Request method and path
- Response status and timing
- Headers (at DEBUG level)
- Bodies (at TRACE level)

**Enable full request/response logging:**
```yaml
logging:
  level:
    com.example.debugdemo.config.RequestLoggingFilter: TRACE
```

---

## 5. Database Debugging

### SQL Query Logging

Already configured in `application.yml`:

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

Sample output:
```sql
Hibernate: 
    select
        p1_0.id,
        p1_0.created_at,
        p1_0.description,
        p1_0.name,
        p1_0.price,
        p1_0.status,
        p1_0.stock,
        p1_0.updated_at 
    from
        products p1_0 
    where
        p1_0.id=?
```

### H2 Console for Database Inspection

1. Go to http://localhost:8080/h2-console
2. Enter JDBC URL: `jdbc:h2:mem:debugdb`
3. Username: `sa`, Password: (empty)
4. Run queries directly:

```sql
-- Check all products
SELECT * FROM products;

-- Find products by status
SELECT * FROM products WHERE status = 'ACTIVE';

-- Check for orphaned data
SELECT * FROM products WHERE stock < 0;
```

### Common Database Issues

| Issue | Symptoms | Debug Steps |
|-------|----------|-------------|
| N+1 Query | Many similar queries | Enable SQL logging, look for loops |
| Slow Query | High latency | Check SQL logs, add indexes |
| Transaction | Data not saving | Check @Transactional annotations |
| Lazy Loading | LazyInitializationException | Fetch in transaction or use DTO |

---

## 6. Exception Debugging

### Exception Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                        Exception Thrown                           │
│                              ↓                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │               GlobalExceptionHandler                         │ │
│  │  • Logs exception with unique error ID                       │ │
│  │  • Creates standardized error response                       │ │
│  │  • Returns appropriate HTTP status                           │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                              ↓                                    │
│                     Client Error Response                         │
│  {                                                                │
│    "errorId": "A1B2C3D4",                                        │
│    "timestamp": "2024-01-15T10:30:00",                           │
│    "status": 404,                                                │
│    "code": "NOT_FOUND",                                          │
│    "message": "Product not found with id: 999"                   │
│  }                                                                │
└──────────────────────────────────────────────────────────────────┘
```

### Finding Exception Details

1. **From API Response:** Note the `errorId`
2. **In Logs:** Search for `Error ID: <errorId>`
3. **Full Stack Trace:** Found in logs for ERROR level

### Exception Breakpoint Setup

In IntelliJ:
1. Run → View Breakpoints (Ctrl+Shift+F8)
2. Click '+' → Java Exception Breakpoints
3. Add: `java.lang.NullPointerException`
4. Add: `com.example.debugdemo.exception.ProductNotFoundException`

---

## 7. Performance Debugging

### Identifying Slow Requests

The `RequestLoggingFilter` logs timing:
```
[A1B2C3D4] <-- 200 OK (1523ms)
[A1B2C3D4] SLOW REQUEST: took 1523ms
```

### Simulating Slow Queries

Enable in `application.yml`:
```yaml
app:
  debug:
    simulate-slow-queries: true
```

### Simulating Random Errors

Enable resilience testing:
```yaml
app:
  debug:
    simulate-random-errors: true
```

### Memory Analysis

Use Actuator:
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

Or check the debug endpoint:
```bash
curl http://localhost:8080/api/products/debug-info
```

---

## 8. Actuator Endpoints

### Essential Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Application health status |
| `/actuator/info` | Application information |
| `/actuator/beans` | All Spring beans |
| `/actuator/mappings` | All request mappings |
| `/actuator/env` | Environment properties |
| `/actuator/loggers` | View/modify log levels |
| `/actuator/metrics` | Application metrics |
| `/actuator/threaddump` | Thread dump |
| `/actuator/heapdump` | Heap dump (downloads file) |

### Usage Examples

```bash
# Health check
curl http://localhost:8080/actuator/health

# See all endpoints
curl http://localhost:8080/actuator/mappings | jq '.contexts.application.mappings.dispatcherServlets'

# Check a specific logger
curl http://localhost:8080/actuator/loggers/com.example.debugdemo

# Get memory metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

---

## 9. Common Issues & Solutions

### Issue: "WhiteLabel Error Page"

**Cause:** Exception not handled properly

**Debug Steps:**
1. Check console for stack trace
2. Set breakpoint in `GlobalExceptionHandler`
3. Look for missing `@RestController` or `@RequestMapping`

### Issue: "404 Not Found" for valid endpoint

**Cause:** Component scanning or mapping issue

**Debug Steps:**
1. Check `/actuator/mappings` for your endpoint
2. Verify `@RestController` annotation
3. Check package structure (must be under main class package)

### Issue: "Empty response" from database

**Cause:** Data not loaded or wrong query

**Debug Steps:**
1. Check H2 console for data
2. Enable SQL logging
3. Verify `@Transactional` on read methods

### Issue: "Validation failed" unexpectedly

**Cause:** `@Valid` annotation catching issues

**Debug Steps:**
1. Check the `ValidationErrorResponse` for field details
2. Review your `@NotNull`, `@Size` annotations
3. Send a minimal valid request first

### Issue: "LazyInitializationException"

**Cause:** Accessing lazy-loaded data outside transaction

**Debug Steps:**
1. Check `@Transactional` boundaries
2. Use `@Transactional(readOnly = true)` for read operations
3. Consider using DTOs instead of entities

---

## 10. Debugging Exercises

### Exercise 1: Basic Breakpoint Navigation

1. Start the app in debug mode
2. Set a breakpoint in `ProductController.getProductById()`
3. Call `GET /api/products/1`
4. Step through to service → repository → back
5. Inspect the `Product` object at each layer

### Exercise 2: Conditional Breakpoint

1. Set a breakpoint in `ProductService.searchProducts()`
2. Right-click → Add condition: `name != null && name.contains("iPhone")`
3. Search for various products
4. Notice it only stops when searching for "iPhone"

### Exercise 3: Exception Flow Tracing

1. Set a breakpoint in `GlobalExceptionHandler.handleProductNotFound()`
2. Call `GET /api/products/99999` (non-existent)
3. Watch the exception flow
4. Note the `errorId` generated
5. Search logs for this `errorId`

### Exercise 4: Validation Debugging

1. Send invalid data:
   ```bash
   curl -X POST http://localhost:8080/api/products \
        -H "Content-Type: application/json" \
        -d '{"name":"","price":-10,"stock":-5}'
   ```
2. Set breakpoint in `GlobalExceptionHandler.handleValidationErrors()`
3. Inspect `MethodArgumentNotValidException` details

### Exercise 5: SQL Query Analysis

1. Enable SQL logging (already enabled)
2. Call search endpoint with various parameters:
   ```bash
   curl "http://localhost:8080/api/products/search?name=phone&minPrice=100"
   ```
3. Observe the generated SQL in console
4. Copy SQL and run in H2 console to verify

### Exercise 6: Performance Issue Simulation

1. Enable slow queries in `application.yml`:
   ```yaml
   app:
     debug:
       simulate-slow-queries: true
   ```
2. Restart application
3. Create a product and watch logs for timing
4. Set breakpoint in `simulateSlowQueryIfEnabled()`

### Exercise 7: Request Tracing

1. Enable TRACE logging for `RequestLoggingFilter`
2. Make several API calls
3. Grep logs by request ID
4. Follow the complete request lifecycle

---

## API Reference

### Products API

```bash
# Get all products
GET /api/products

# Get product by ID
GET /api/products/{id}

# Search products
GET /api/products/search?name=phone&minPrice=100&maxPrice=500&status=ACTIVE

# Create product
POST /api/products
Content-Type: application/json
{"name":"Product Name","description":"Description","price":99.99,"stock":10}

# Update product
PUT /api/products/{id}
Content-Type: application/json
{"name":"New Name","price":149.99}

# Update stock
PATCH /api/products/{id}/stock?change=-5

# Delete product
DELETE /api/products/{id}

# Discontinue product
POST /api/products/{id}/discontinue

# Health check
GET /api/products/health

# Debug info
GET /api/products/debug-info
```

---

## Project Structure

```
springboot-debug-tutorial/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/debugdemo/
│   │   │   ├── DebugDemoApplication.java    # Entry point
│   │   │   ├── config/
│   │   │   │   ├── DataInitializer.java     # Sample data
│   │   │   │   └── RequestLoggingFilter.java # Request tracing
│   │   │   ├── controller/
│   │   │   │   └── ProductController.java   # REST endpoints
│   │   │   ├── exception/
│   │   │   │   ├── BusinessLogicException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ProductNotFoundException.java
│   │   │   ├── model/
│   │   │   │   ├── Product.java             # Entity
│   │   │   │   └── ProductStatus.java       # Enum
│   │   │   ├── repository/
│   │   │   │   └── ProductRepository.java   # Data access
│   │   │   └── service/
│   │   │       └── ProductService.java      # Business logic
│   │   └── resources/
│   │       └── application.yml              # Configuration
│   └── test/
│       └── java/
└── README.md
```

---

## Need Help?

1. **Check the logs first!** Most answers are in the logs.
2. **Set strategic breakpoints** in GlobalExceptionHandler.
3. **Use H2 console** to verify database state.
4. **Use Actuator endpoints** for runtime inspection.
5. **Enable TRACE logging** for deep debugging.

Happy Debugging! 🐛→🦋
